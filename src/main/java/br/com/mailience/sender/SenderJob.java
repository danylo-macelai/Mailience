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
package br.com.mailience.sender;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.mailience.email.EmailService;
import br.com.mailience.email.EmailStatus;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsável por executar, de forma agendada, o envio de e-mails que estão pendentes ou em tentativa de reenvio.
 *
 * <p>
 * Garante que apenas uma execução do processo ocorra por vez, evitando concorrência entre execuções simultâneas.
 * </p>
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "mailience.job.enabled", havingValue = "true")
class SenderJob {

    private final ReentrantLock lock = new ReentrantLock();
    private final EmailService  emailService;
    private final Executor      senderExecutor;

    SenderJob(
            final EmailService emailService,
            @Qualifier("senderExecutor") final Executor senderExecutor) {
        this.emailService = emailService;
        this.senderExecutor = senderExecutor;
    }

    /**
     * Executa o processo de envio dos e-mails pendentes, impedindo que múltiplas execuções concorrentes ocorram. Busca
     * os e-mails com status pendente ou em reprocessamento e aciona o envio.
     */
    @Scheduled( //
            fixedDelayString = "${mailience.job.fixedDelay}", //
            initialDelayString = "${mailience.job.initialDelay}", //
            timeUnit = TimeUnit.SECONDS //
    )
    void run() {
        if (lock.tryLock()) {
            try {
                log.info("📧 Iniciando o job de envio de e-mails pendentes...");
                var emails = emailService.findPending(EmailStatus.PENDING, EmailStatus.RETRYING);
                if (!emails.isEmpty()) {
                    log.info("🕒 {} e-mails encontrados para envio. Iniciando processamento paralelo...",
                            emails.size());

                    var futures = emails.stream()
                            .map(email -> CompletableFuture.runAsync(() -> emailService.send(email), senderExecutor))
                            .toList();

                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                } else {
                    log.info("✔️ Nenhum e-mail pendente encontrado para envio.");
                }
            } catch (Exception ex) {
                log.error("❌ Falha ao executar o job de envio de e-mails: {}", ex.getMessage(), ex);
            } finally {
                lock.unlock();
                log.info("🏁 Job de envio finalizado, lock liberado.");
            }
        } else {
            log.info("⚠️ O job de envio já está em execução. Esta execução foi ignorada para evitar concorrência.");
        }
    }

}
