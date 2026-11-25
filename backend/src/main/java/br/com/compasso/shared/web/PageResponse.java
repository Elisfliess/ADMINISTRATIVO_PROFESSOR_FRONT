package br.com.compasso.shared.web;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * Metadados de paginação exigidos pelo contrato HTTP. Mantém os mesmos nomes
 * de campo para todos os endpoints paginados e evita expor {@link Page}
 * diretamente, cuja serialização varia entre versões do Spring Data.
 */
public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last) {

    public static <E, T> PageResponse<T> de(Page<E> pagina, Function<E, T> conversor) {
        return new PageResponse<>(
                pagina.getContent().stream().map(conversor).toList(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pagina.isFirst(),
                pagina.isLast());
    }
}
