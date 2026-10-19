package com.littlehorizons.erp.service;

import com.littlehorizons.erp.dao.LoanDao;
import com.littlehorizons.erp.dao.ResourceDao;
import com.littlehorizons.erp.dao.TransactionManager;
import com.littlehorizons.erp.exception.BusinessRuleException;
import com.littlehorizons.erp.exception.ValidationException;
import com.littlehorizons.erp.model.resources.Resource;
import com.littlehorizons.erp.model.resources.ResourceCategory;
import com.littlehorizons.erp.model.resources.ResourceStatus;
import com.littlehorizons.erp.util.InputValidator;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Inventario de recursos. El estado "Prestado" solo lo maneja
 * {@link LoanService}; un recurso prestado no se puede enviar a mantenimiento ni
 * dar de baja hasta que se devuelva.
 */
public class ResourceService {

    private static final int SEARCH_LIMIT = 500;

    private final TransactionManager transactions;
    private final ResourceDao resourceDao;
    private final LoanDao loanDao;
    private final Clock clock;

    public ResourceService(TransactionManager transactions, ResourceDao resourceDao, LoanDao loanDao, Clock clock) {
        this.transactions = Objects.requireNonNull(transactions, "transactions");
        this.resourceDao = Objects.requireNonNull(resourceDao, "resourceDao");
        this.loanDao = Objects.requireNonNull(loanDao, "loanDao");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /** Categorías de recursos del catálogo. */
    public List<ResourceCategory> findCategories() {
        return transactions.read(resourceDao::findCategories);
    }

    /**
     * Registra un recurso nuevo con estado "Disponible" después de validar que el código no esté repetido.
     */
    public Resource register(Resource draft) {
        Resource resource = normalize(draft).withStatus(ResourceStatus.AVAILABLE);
        return transactions.inTransaction(connection -> {
            if (resourceDao.findByCode(connection, resource.code()).isPresent()) {
                throw new BusinessRuleException("Ya existe un recurso con el código " + resource.code() + ".");
            }
            checkCategory(resourceDao.findCategories(connection), resource.categoryId());
            int id = resourceDao.insert(connection, resource);
            return resourceDao.findById(connection, id).orElseThrow();
        });
    }

    /** Actualiza datos y estado. "Prestado" no se puede poner ni quitar aquí. */
    public Resource update(Resource changes) {
        Objects.requireNonNull(changes.id(), "id");
        Resource resource = normalize(changes);
        return transactions.inTransaction(connection -> {
            Resource current = ServiceChecks.require(resourceDao.lockById(connection, resource.id()),
                    "No existe un recurso con id " + resource.id() + ".");
            checkCategory(resourceDao.findCategories(connection), resource.categoryId());
            resourceDao.findByCode(connection, resource.code())
                    .filter(existing -> !existing.id().equals(resource.id()))
                    .ifPresent(existing -> {
                        throw new BusinessRuleException("El código " + resource.code() + " ya pertenece a otro recurso.");
                    });
            boolean statusChanged = current.status() != resource.status();
            if (statusChanged && (current.status() == ResourceStatus.LOANED || resource.status() == ResourceStatus.LOANED)) {
                throw new BusinessRuleException("El estado Prestado solo cambia al registrar un préstamo o su devolución.");
            }
            if (statusChanged && !loanDao.findOpenByResource(connection, resource.id()).isEmpty()) {
                throw new BusinessRuleException("El recurso tiene un préstamo sin devolver.");
            }
            resourceDao.update(connection, resource);
            return resourceDao.findById(connection, resource.id()).orElseThrow();
        });
    }

    /** Busca un recurso por su código o lanza un error si no existe. */
    public Resource findByCode(String code) {
        String normalized = InputValidator.validateCode(code, "código", 50);
        return transactions.read(connection -> ServiceChecks.require(resourceDao.findByCode(connection, normalized),
                "No existe un recurso con el código " + normalized + "."));
    }

    /** Busca recursos por texto, categoría y estado (los filtros null se ignoran). */
    public List<Resource> search(String text, Integer categoryId, ResourceStatus status) {
        return transactions.read(connection -> resourceDao.search(connection, text, categoryId, status, SEARCH_LIMIT));
    }

    /** Cantidad de recursos por categoría y estado, para el reporte de disponibilidad. */
    public Map<String, Map<ResourceStatus, Long>> availabilityByCategory() {
        List<Resource> resources = search(null, null, null);
        return resources.stream().collect(Collectors.groupingBy(Resource::categoryName, TreeMap::new,
                Collectors.groupingBy(Resource::status, () -> new java.util.EnumMap<>(ResourceStatus.class),
                        Collectors.counting())));
    }

    /** Verifica que la categoría exista en el catálogo. */
    private static void checkCategory(List<ResourceCategory> categories, int categoryId) {
        if (categories.stream().noneMatch(category -> category.id() == categoryId)) {
            throw new BusinessRuleException("No existe una categoría con id " + categoryId + ".");
        }
    }

    /** Valida y limpia los datos del recurso (código, nombre, fecha de adquisición no futura). */
    private Resource normalize(Resource resource) {
        String code = InputValidator.validateCode(resource.code(), "código del recurso", 50);
        String name = InputValidator.requireText(resource.name(), "nombre del recurso", 150);
        if (resource.acquisitionDate() != null && resource.acquisitionDate().isAfter(LocalDate.now(clock))) {
            throw new ValidationException("La fecha de adquisición no puede ser futura.");
        }
        if (resource.status() == null && resource.id() != null) {
            throw new ValidationException("El estado del recurso es obligatorio.");
        }
        return new Resource(resource.id(), code, name, resource.categoryId(), resource.categoryName(),
                InputValidator.optionalText(resource.description(), "descripción", 2000), resource.acquisitionDate(),
                InputValidator.optionalText(resource.location(), "ubicación", 100), resource.status());
    }
}
