package org.devlive.metaforge.service.service.impl;

import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.service.DataSetColumnService;
import org.springframework.stereotype.Service;

@Service
public class DataSetColumnServiceImpl
        implements DataSetColumnService
{
    private final DataSetRepository repository;

    public DataSetColumnServiceImpl(DataSetRepository repository)
    {
        this.repository = repository;
    }
}
