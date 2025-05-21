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

import java.util.List;

/**
 * Serviço responsável pelo gerenciamento de e-mails.
 *
 * <p>
 * Define as operações de leitura e gravação de e-mails no repositório, usadas durante o processo de envio.
 * </p>
 */
public interface EmailService {

    /**
     * Busca os e-mails com status pendente ou em reprocessamento, usando paginação.
     *
     * @param statuses status a serem considerados na busca
     * @return lista paginada de e-mails a serem processados
     */
    List<EmailTO> findPending(final EmailStatus... statuses);

    /**
     * Salva um e-mail no banco de dados.
     *
     * @param email entidade a ser persistida
     * @return e-mail salvo com ID e timestamps atualizados
     */
    EmailTO save(final EmailTO email);

    /**
     * Realiza o envio em lote de e-mails utilizando o {@link org.springframework.mail.javamail.JavaMailSenderImpl}.
     *
     * <p>
     * Este método recebe uma lista de e-mails e os converte para instâncias de
     * {@link jakarta.mail.internet.MimeMessage}, enviando todas de uma vez por meio do
     * {@code JavaMailSenderImpl#send(MimeMessage...)}. Isso permite o reuso de conexão SMTP e reduz drasticamente o
     * tempo total de envio, em comparação com o envio individual de cada e-mail.
     * </p>
     *
     * <p>
     * O envio em lote é mais eficiente, escalável e apropriado para sistemas que disparam notificações em massa. Em
     * caso de falhas parciais, os e-mails com erro são identificados e marcados como {@link EmailStatus#RETRYING} ou
     * {@link EmailStatus#FAILED}, de acordo com a quantidade de tentativas. Já os e-mails enviados com sucesso são
     * atualizados para o status {@link EmailStatus#SENT}.
     * </p>
     *
     * <p>
     * O uso do {@code JavaMailSender} com arrays de {@code MimeMessage} é altamente recomendado para maximizar o
     * throughput do sistema de envio, reduzir carga sobre o servidor SMTP e manter a consistência dos envios.
     * </p>
     *
     * @param jobExecutionId Identificador do lote de envio (UUID do job que processou este e-mail)
     * @param batch lista de e-mails a serem enviados
     * @throws RuntimeException em caso de erro crítico ao criar ou enviar as mensagens
     */
    void send(final String jobExecutionId, final List<EmailTO> batch);

}
