import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /api/v1/products/{id} debe retornar 200 con el producto"
    request {
        method GET()
        url '/api/v1/products/1'
        headers { header('Authorization', 'Bearer test-token') }
    }
    response {
        status 200
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
