import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "POST /api/v1/products debe retornar 201 al crear producto"
    request {
        method POST()
        url '/api/v1/products'
        headers {
            header('Authorization', 'Bearer test-token')
            contentType(applicationJson())
        }
        body([
            name      : "New Product",
            sku       : "NEW-001",
            category  : "Electronics",
            price     : 99.99,
            quantity  : 10,
            minStock  : 3
        ])
    }
    response {
        status 201
        headers { contentType(applicationJson()) }
        body([
            id        : $(anyPositiveInt()),
            name      : $(anyNonEmptyString()),
            sku       : $(anyNonEmptyString()),
            category  : $(anyNonEmptyString()),
            price     : $(anyNumber()),
            quantity  : $(anyPositiveInt()),
            minStock  : $(anyPositiveInt()),
            status    : $(anyOf('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
            lowStock  : $(anyBoolean()),
            createdAt : $(anyIso8601WithOffset())
        ])
    }
}
