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

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Entidade que representa um e-mail armazenado e gerenciado pelo sistema.
 *
 * <p>
 * Contém os dados essenciais para envio de e-mails como destinatário, assunto, corpo, status e tentativas.
 * </p>
 */
@Entity
@Table( //
        name = "MF_EMAIL", //
        indexes = {
                @Index(name = "idx_email_recipient", columnList = "recipient"),
                @Index(name = "idx_email_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailTO {

    /**
     * Identificador único do e-mail (chave primária).
     */
    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "id", updatable = false, nullable = false)
    private Long id;

    /**
     * Endereço de e-mail do destinatário.
     */
    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    /**
     * Assunto do e-mail.
     */
    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    /**
     * Corpo da mensagem do e-mail.
     */
    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    /**
     * Status atual do e-mail no fluxo de envio.
     */
    @Enumerated(STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus status;

    /**
     * Número de tentativas de envio já realizadas.
     */
    @Column(name = "attempts", nullable = false)
    private int attempts;

    /**
     * Identificador do lote de envio (UUID do job que processou este e-mail).
     */
    @Column(name = "job_execution_id", nullable = true, length = 255)
    private String jobExecutionId;

}
