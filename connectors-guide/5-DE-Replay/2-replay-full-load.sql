UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Category.Created')
    ) inner_query
    where inner_query.rn = 1);


UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Category.Updated')
    ) inner_query
    where inner_query.rn = 1);




UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.Created')
    ) inner_query
    where inner_query.rn = 1)


UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.Updated')
    ) inner_query
    where inner_query.rn = 1)



UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.SpecificationUpdated')
    ) inner_query
    where inner_query.rn = 1)


UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.RelationUpdated')
    ) inner_query
    where inner_query.rn = 1)


UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.AssetUpdated')
    ) inner_query
    where inner_query.rn = 1)


UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.VariantRelationUpdated')
    ) inner_query
    where inner_query.rn = 1)


UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.CategoryUpdated')
    ) inner_query
    where inner_query.rn = 1)



UPDATE catalog.outbox_event set time = now() WHERE ID IN ( select id from
    (select t.*, row_number() over(partition by aggregate_id, type order by time desc) rn
     from catalog.outbox_event t
     where (t.type ='Product.ModelAndMachineRelationUpdated')
    ) inner_query
    where inner_query.rn = 1)

