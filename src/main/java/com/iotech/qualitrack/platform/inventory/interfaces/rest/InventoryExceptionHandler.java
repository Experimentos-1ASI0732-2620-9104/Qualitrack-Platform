package com.iotech.qualitrack.platform.inventory.interfaces.rest;

import com.iotech.qualitrack.platform.shared.application.result.ApplicationError;
import com.iotech.qualitrack.platform.shared.interfaces.rest.transform.ErrorResponseAssembler;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Concurrent changes and duplicate inventory records answer 409 with the standard error body (INVENTORY_CONFLICT).
 */
@Order(0)
@RestControllerAdvice(assignableTypes = {InventoryController.class, EnvironmentInventoryController.class})
public class InventoryExceptionHandler {
    @ExceptionHandler({DataIntegrityViolationException.class, PessimisticLockingFailureException.class})
    public ResponseEntity<?> conflict(RuntimeException exception) {
        return ErrorResponseAssembler.toErrorResponseFromApplicationError(ApplicationError.conflict("Inventory",
                "Concurrent change or duplicate inventory record. Reload and retry with the same operation ID."));
    }
}
