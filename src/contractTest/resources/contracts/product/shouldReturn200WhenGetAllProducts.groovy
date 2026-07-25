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
                    id        : $(anyNumber()),
                    name      : $(anyNonEmptyString()),
                    sku       : $(anyNonEmptyString()),
                    category  : $(anyNonEmptyString()),
                    price     : $(anyNumber()),
                    quantity  : $(anyNumber()),
                    minStock  : $(anyNumber()),
                    status    : $(anyOf('ACTIVE', 'INACTIVE', 'DISCONTINUED')),
                    lowStock  : $(anyBoolean()),
                    createdAt : $(anyNonEmptyString())
                ]
            ],
            page          : $(anyNumber()),
            size          : $(anyNumber()),
            totalElements : $(anyNumber()),
            totalPages    : $(anyNumber()),
            last          : $(anyBoolean())
        ])
    }
}


