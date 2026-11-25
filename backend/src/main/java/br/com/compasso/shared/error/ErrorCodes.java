package br.com.compasso.shared.error;

/**
 * Catálogo de códigos estáveis devolvidos no campo {@code code} do Problem
 * Details. Códigos não devem ser renomeados depois de publicados; novos
 * códigos podem ser adicionados conforme os fluxos forem implementados.
 */
public final class ErrorCodes {

    // Erros genéricos de transporte e validação.
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String MALFORMED_JSON = "MALFORMED_JSON";
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    // Autenticação e conta.
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INVALID_CREDENTIALS = "INVALID_CREDENTIALS";
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    public static final String ACCOUNT_DISABLED = "ACCOUNT_DISABLED";
    public static final String ACCOUNT_LOCKED = "ACCOUNT_LOCKED";
    public static final String TOKEN_INVALID_OR_EXPIRED = "TOKEN_INVALID_OR_EXPIRED";
    public static final String RESEND_TOO_SOON = "RESEND_TOO_SOON";
    public static final String RESEND_LIMIT_EXCEEDED = "RESEND_LIMIT_EXCEEDED";

    // Política de senha.
    public static final String PASSWORD_TOO_SHORT = "PASSWORD_TOO_SHORT";
    public static final String PASSWORD_TOO_LONG = "PASSWORD_TOO_LONG";
    public static final String PASSWORD_TOO_WEAK = "PASSWORD_TOO_WEAK";

    // Documentos legais.
    public static final String LEGAL_ACCEPTANCE_REQUIRED = "LEGAL_ACCEPTANCE_REQUIRED";
    public static final String LEGAL_VERSION_NOT_FOUND = "LEGAL_VERSION_NOT_FOUND";

    // Professor.
    public static final String CNPJ_INVALID = "CNPJ_INVALID";

    private ErrorCodes() {
    }
}
