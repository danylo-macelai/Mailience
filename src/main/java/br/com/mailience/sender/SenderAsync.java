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

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do executor assíncrono utilizado para processar tarefas de envio de e-mails.
 *
 * <p>
 * Define um {@link ThreadPoolExecutor} personalizado com parâmetros configuráveis por propriedades externas, permitindo
 * controle sobre o número de threads, tempo de vida ociosa e fila de tarefas.
 * </p>
 */
@Configuration
class SenderAsync {

    @Value("${mailience.executor.core-pool-size}")
    private int corePoolSize;

    @Value("${mailience.executor.maximum-pool-size}")
    private int maximumPoolSize;

    @Value("${mailience.executor.keep-alive-time}")
    private long keepAliveTime;

    @Value("${mailience.executor.work-queue}")
    private int workQueue;

    @Value("${mailience.executor.name}")
    private String name;

    /**
     * Cria e expõe um bean do tipo {@link Executor} configurado para uso assíncrono no envio de e-mails.
     *
     * <p>
     * Utiliza política {@code CallerRunsPolicy} como fallback e atribui nomes personalizados às threads para facilitar
     * o monitoramento.
     * </p>
     *
     * @return executor configurado para tarefas assíncronas de envio
     */
    @Bean(name = "senderExecutor")
    Executor senderExecutor() {
        var counter = new AtomicInteger(1);
        ThreadFactory threadFactory = runnable -> {
            var thread = new Thread(runnable);
            thread.setName(name + counter.getAndIncrement());
            return thread;
        };

        return new ThreadPoolExecutor(
                corePoolSize,
                maximumPoolSize,
                keepAliveTime, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(workQueue),
                threadFactory,
                new ThreadPoolExecutor.CallerRunsPolicy());
    }
}