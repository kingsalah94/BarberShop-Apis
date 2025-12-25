package com.salahtech.BarberShop_Apis.Exceptions;

/**
 * Exception levée lors d'une erreur d'upload de fichier
 */
public class FileUploadException extends RuntimeException {

    public FileUploadException(String message) {
        super(message);
    }

    public FileUploadException(String message, Throwable cause) {
        super(message, cause);
    }
}
