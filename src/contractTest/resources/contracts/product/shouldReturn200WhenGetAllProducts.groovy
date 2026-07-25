import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "GET /api/v1/products debe retornar 200 con lista paginada de productos"
    request {
        method GET()
        url '/api/v1/products'
        headers { header('Authorization', 'Bearer test-token') }
    }
    response {
        status 200
        headers { contentType(applicationJson()) }
        body([
            content: [
                [
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
                ]
            ],
            page          : $(anyPositiveInt()),
            size          : $(anyPositiveInt()),
            totalElements : $(anyPositiveInt()),
            totalPages    : $(anyPositiveInt()),
            last          : $(anyBoolean())
        ])
    }
}
