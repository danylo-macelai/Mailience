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

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Repositório JPA para acesso e manipulação de e-mails.
 *
 * <p>
 * Fornece operações de persistência e consulta customizada com base no status dos e-mails.
 * </p>
 */
interface EmailRepository extends JpaRepository<EmailTO, Long> {

    /**
     * Recupera uma lista paginada de e-mails cujo status está contido na lista fornecida.
     *
     * @param statuses Lista de status dos e-mails a serem buscados (ex: PENDING, FAILED).
     * @param pageable Objeto {@link Pageable} para controle de paginação e ordenação.
     * @return Lista de e-mails com os status informados, respeitando os limites de paginação.
     */
    List<EmailTO> findByStatusIn(final List<EmailStatus> statuses, final Pageable pageable);

    /**
     * Atualiza os campos de tentativas de envio e status de envio de um e-mail específico.
     *
     * <p>
     * Este método utiliza uma consulta JPQL personalizada para atualizar os campos:
     * <ul>
     * <li>{@code attempts}: Número de tentativas de envio falhas.</li>
     * <li>{@code status}: Status de envio do e-mail.</li>
     * </ul>
     * </p>
     *
     * @param email o objeto {@link EmailTO} contendo os dados a serem atualizados. Deve incluir o identificador
     *            {@code id} e os campos {@code attempts} e {@code status}.
     */
    @Modifying
    @Query(nativeQuery = false, //
            value = """
                     UPDATE EmailTO E
                     SET E.attempts = :#{#email.attempts},
                         E.status = :#{#email.status}
                     WHERE E.id = :#{#email.id}
                    """)
    void update(@Param("email") EmailTO email);

}
