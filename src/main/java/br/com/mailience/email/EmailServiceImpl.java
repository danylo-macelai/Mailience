/*
 * MIT License
 *
 * Copyright (c) 2025.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package br.com.mailience.email;

import static br.com.mailience.email.EmailStatus.FAILED;
import static br.com.mailience.email.EmailStatus.RETRYING;
import static br.com.mailience.email.EmailStatus.SENT;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

/**
 * Implementacao do servico de gerenciamento de e-mails.
 *
 * <p>
 * Responsavel por buscar e-mails pendentes no repositório, persistir novos registros e realizar o envio por meio do
 * {@link JavaMailSender}. Implementa a logica de tratamento para marcar e-mails como enviados ou em reprocessamento em
 * caso de falha.
 * </p>
 */
@Service
@Slf4j
public class EmailServiceImpl implements EmailService {

    private static final String       HEADER_EMAIL_ID = "X-Email-ID";
    private final int                 pageSize;
    private final String              from;
    private final int                 maxAttempts;
    private final JavaMailSender      mailSender;
    private final EmailRepository     repository;
    private final TransactionTemplate transactionTemplate;

    EmailServiceImpl(@Value("${mailience.executor.work-queue}") final int pageSize,
            @Value("${mailience.mail.from}") final String from,
            @Value("${mailience.mail.max.attempts}") final int maxAttempts,
            final JavaMailSender mailSender,
            final EmailRepository repository,
            final TransactionTemplate transactionTemplate) {
        this.pageSize = pageSize;
        this.from = from;
        this.maxAttempts = maxAttempts;
        this.mailSender = mailSender;
        this.repository = repository;
        this.transactionTemplate = transactionTemplate;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<EmailTO> findPending(final EmailStatus... statuses) {
        var pageRequest = PageRequest.of(0, pageSize, Sort.by("id").ascending());
        return repository.findByStatusIn(Arrays.asList(statuses), pageRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public EmailTO save(final EmailTO email) {
        return repository.save(email);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @CircuitBreaker(name = "emailServiceSend", fallbackMethod = "sendFallback")
    public void send(final String jobExecutionId, final List<EmailTO> batch) {
        final Set<String> failedIds = new HashSet<>();
        try {
            var messages = batch.stream()
                    .map(this::toMimeMessage)
                    .toArray(MimeMessage[]::new);

            mailSender.send(messages);
            log.info("✅ {} e-mails enviados com sucesso.", batch.size());

        } catch (MailSendException mse) {
            mse.getFailedMessages().keySet().stream()
                    .map(msg -> extractHeader((MimeMessage) msg, HEADER_EMAIL_ID))
                    .filter(Objects::nonNull)
                    .forEach(failedIds::add);

            log.error("❌ Alguns e-mails falharam no envio: {}", failedIds);
            throw mse;
        } finally {
            for (var email : batch) {
                email.setJobExecutionId(jobExecutionId);
                if (failedIds.contains(email.getId().toString())) {
                    markAsFailed(email);
                } else {
                    markAsSent(email);
                }
            }
        }
    }

    /**
     * Fallback acionado pelo Circuit Breaker quando o método send falha ou o circuito está aberto.
     *
     * @param batch lote de e-mails que não puderam ser enviados
     * @param t exceção que causou o fallback
     */
    public void sendFallback(final String jobExecutionId, final List<EmailTO> batch, final Throwable t) {
        log.info("CircuitBreaker acionado - envio de e-mails temporariamente bloqueado");
        restoreToRetryingIfFailed(jobExecutionId);
    }

    /**
     * Converte um {@link EmailTO} em um {@link MimeMessage} pronto para envio.
     *
     * @param email e-mail a ser convertido
     * @return mensagem formatada com headers e conteudo
     */
    private MimeMessage toMimeMessage(final EmailTO email) {
        try {
            var message = mailSender.createMimeMessage();
            message.setHeader(HEADER_EMAIL_ID, email.getId().toString());

            var helper = new MimeMessageHelper(message, true, UTF_8.name());

            helper.setFrom(from);
            helper.setTo(email.getRecipient());
            helper.setSubject(email.getSubject());
            helper.setText(email.getBody(), true);

            return message;
        } catch (Exception e) {
            log.error("Erro ao criar MimeMessage para ID {}", email.getId(), e);
            throw new RuntimeException("Erro ao criar mensagem", e);
        }
    }

    /**
     * Extrai o valor de um header especifico de uma {@link MimeMessage}.
     *
     * @param msg mensagem de onde o header sera extraido
     * @param name nome do header
     * @return valor do header, ou {@code null} se nao encontrado
     */
    private String extractHeader(final MimeMessage msg, final String name) {
        try {
            return msg.getHeader(name, null);
        } catch (Exception e) {
            log.warn("⚠️ Erro ao ler header '{}'", name, e);
            return null;
        }
    }

    /**
     * Marca o e-mail como enviado e persiste a alteracao.
     *
     * @param email e-mail a ser atualizado
     */
    private void markAsSent(final EmailTO email) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                email.setAttempts(email.getAttempts() + 1);
                email.setStatus(SENT);
                repository.update(email);
            }
        });
    }

    /**
     * Marca o e-mail para reprocessamento (RETRYING) e persiste a alteracao.
     *
     * @param email e-mail a ser atualizado
     */
    private void markAsFailed(final EmailTO email) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                email.setAttempts(email.getAttempts() + 1);
                if (email.getAttempts() < maxAttempts) {
                    email.setStatus(RETRYING);
                } else {
                    email.setStatus(FAILED);
                }
                repository.update(email);
            }
        });
    }

    /**
     * Reativa e-mails marcados como {@link EmailStatus#FAILED}, dando a eles uma nova chance de serem reprocessados.
     *
     * Essa lógica é usada como fallback do Circuit Breaker para permitir uma última tentativa de envio em situações
     * temporárias de falha.
     *
     * @param jobExecutionId identificador do job cujos e-mails serão atualizados
     */
    private void restoreToRetryingIfFailed(final String jobExecutionId) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(final TransactionStatus status) {
                repository.restoreToRetryingIfFailed(maxAttempts - 1, RETRYING, jobExecutionId);
            }
        });
    }

}