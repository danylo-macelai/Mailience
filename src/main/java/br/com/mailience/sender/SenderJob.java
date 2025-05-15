package br.com.mailience.sender;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import br.com.mailience.email.EmailService;
import br.com.mailience.email.EmailStatus;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
@ConditionalOnProperty(name = "mailience.job.enabled", havingValue = "true")
public class SenderJob {

    private final EmailService  emailService;
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Executa o processo de envio dos e-mails pendentes, impedindo que múltiplas execuções concorrentes ocorram. Busca
     * os e-mails com status pendente ou em reprocessamento e aciona o envio.
     */
    @Scheduled( //
            fixedDelayString = "${mailience.job.fixedDelay}", //
            initialDelayString = "${mailience.job.initialDelay}", //
            timeUnit = TimeUnit.SECONDS //
    )
    public void run() {
        if (lock.tryLock()) {
            try {
                log.info("📧 Iniciando job de envio de e-mails pendentes...");

                var emails = emailService.findPending(EmailStatus.PENDING, EmailStatus.RETRYING);
                if (emails.isEmpty()) {
                    log.info("Nenhum e-mail pendente encontrado.");
                    return;
                }

                emails.forEach(System.out::println);

                log.info("✅ Job de envio de e-mails finalizado.");
            } catch (Exception ex) {
                log.error("Erro ao executar job de envio de e-mails: {}", ex.getMessage(), ex);
            } finally {
                lock.unlock();
            }
        } else {
            log.info("⚠️ Job já está em execução, pulando esta execução...");
        }
    }
}
