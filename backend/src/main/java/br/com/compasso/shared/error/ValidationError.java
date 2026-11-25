package br.com.compasso.shared.error;

/** Item da extensão {@code errors} do Problem Details. */
public record ValidationError(String field, String code, String message) {
}
