import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "PUT /api/v1/products/{id} debe retornar 200 al actualizar"
    request {
        method PUT()
        url '/api/v1/products/1'
        headers {
            header('Authorization', 'Bearer test-token')
            contentType(applicationJson())
        }
        body([
            name      : "Updated Product",
            sku       : "UPD-001",
            category  : "Electronics",
            price     : 150.00,
            quantity  : 20,
            minStock  : 5
        ])
    }
    response {
        status 200
        headers { contentType(applicationJson()) }
        body([
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
        ])
    }
}


