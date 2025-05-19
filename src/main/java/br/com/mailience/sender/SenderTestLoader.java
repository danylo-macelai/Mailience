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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import br.com.mailience.email.EmailService;
import br.com.mailience.email.EmailStatus;
import br.com.mailience.email.EmailTO;
import lombok.extern.slf4j.Slf4j;

/**
 * Responsavel por inserir dados iniciais no banco de dados apos o startup da aplicacao.
 *
 * <p>
 * A carga inicial consiste em 500 e-mails HTML com status {@code PENDING}, enviados para o destinatario definido em
 * {@code mailience.test.recipient}. A execucao ocorre apenas se a propriedade estiver presente e nenhum e-mail pendente
 * ja existir.
 * </p>
 *
 * <p>
 * Essa classe e ativada condicionalmente por {@link ConditionalOnProperty}, garantindo que apenas ambientes de teste ou
 * validacao populam a base com dados mockados.
 * </p>
 */
@Component
@ConditionalOnProperty(name = "mailience.sender.test.loader")
@Slf4j
class SenderTestLoader {

    private final EmailService service;
    private final String       recipient;

    SenderTestLoader(
            final EmailService service,
            @Value("${mailience.sender.test.loader}") final String recipient) {
        this.service = service;
        this.recipient = recipient;
    }

    /**
     * Evento disparado quando a aplicacao estiver pronta ({@link ApplicationReadyEvent}).
     *
     * <p>
     * Insere 500 e-mails HTML no repositório, se e somente se nenhum e-mail pendente ou em reprocessamento estiver
     * presente. Cada e-mail gerado e associado ao destinatario configurado via {@code mailience.test.recipient}.
     * </p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seedInitialData() {
        if (service.findPending(EmailStatus.values()).size() > 0) {
            log.info("📨 Emails já existem no banco. Ignorando carga inicial.");
            return;
        }

        for (int i = 1; i <= 500; i++) {
            var email = new EmailTO(
                    null,
                    recipient,
                    "Mensagem de Boas-vindas #" + i,
                    """
                            <html>
                                <body>
                                    <h2>Bem-vindo, Usuário #%d!</h2>
                                    <p>Este é um e-mail de teste com conteúdo <strong>HTML</strong>.</p>
                                    <p>Obrigado por participar do nosso sistema de notificações.</p>
                                    <hr/>
                                    <small>Este é um envio automático. Não responda.</small>
                                </body>
                            </html>
                            """.formatted(i),
                    EmailStatus.PENDING,
                    0);
            service.save(email);
        }

        log.info("✅ 500 e-mails com HTML inseridos como carga inicial.");
    }
}