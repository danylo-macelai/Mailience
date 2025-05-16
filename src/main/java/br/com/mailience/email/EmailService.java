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
     * Busca e-mails com status pendente ou em falha para processamento em lote.
     *
     * @param statuses um ou mais status para filtro (ex: PENDING, RETRYING)
     * @return lista de e-mails correspondentes aos status informados
     */
    List<EmailTO> findPending(final EmailStatus... statuses);

    /**
     * Persiste um e-mail no banco de dados.
     *
     * @param email e-mail a ser salvo
     * @return entidade persistida com ID e timestamps atualizados
     */
    EmailTO save(final EmailTO email);

    /**
     * Realiza o envio de um e-mail.
     *
     * @param email e-mail a ser enviado
     */
    void send(final EmailTO email);
}
