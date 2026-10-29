package org.devlive.metaforge.server.service;

import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.service.SelfException;
import org.devlive.metaforge.service.entity.DataSetColumnEntity;
import org.devlive.metaforge.service.entity.DataSetEntity;
import org.devlive.metaforge.service.repository.DataSetColumnRepository;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.impl.DataSetServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

public class DatasetAccessTest
{
    private final DataSetRepository datasets = mock(DataSetRepository.class);
    private final DataSetColumnRepository columns = mock(DataSetColumnRepository.class);
    private final PluginManager plugins = mock(PluginManager.class);
    private final DataSetServiceImpl service = new DataSetServiceImpl(datasets, columns, null, plugins, null, null, null, null, null, null, null);

    @BeforeEach
    public void authenticate()
    {
        UserDetailsService user = new UserDetailsService(1L, "USER", "owner", "", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
    }

    @AfterEach
    public void clear()
    {
        SecurityContextHolder.clearContext();
    }

    private DataSetEntity dataset(long owner)
    {
        return DataSetEntity.builder().id(7L).code("TARGET").userId(owner)
                .columns(Set.of(DataSetColumnEntity.builder().name("amount").id(42L).build())).build();
    }

    @Test
    public void previewRejectsAnotherUsersDatasetBeforeTouchingStorage()
    {
        when(datasets.findByCode("TARGET")).thenReturn(Optional.of(dataset(2)));
        assertThrows(SelfException.class, () -> service.preview("TARGET"));
        verifyNoInteractions(columns, plugins);
    }

    @Test
    public void replacingQueryDefinitionRejectsAnotherUsersDataset()
    {
        when(datasets.findById(7L)).thenReturn(Optional.of(dataset(2)));
        assertThrows(SelfException.class, () -> service.saveOrUpdate(datasets, dataset(1)));
        verify(datasets, never()).save(any());
        verifyNoInteractions(columns, plugins);
    }

    @Test
    public void replacementCannotReuseColumnsFromADifferentDataset()
    {
        DataSetEntity original = dataset(1);
        when(datasets.findById(7L)).thenReturn(Optional.of(original));
        when(columns.findAllByDatasetId(7L)).thenReturn(List.of(DataSetColumnEntity.builder().id(99L).name("owned").build()));
        assertFalse(service.saveOrUpdate(datasets, dataset(1)).getStatus());
        verify(datasets, never()).save(any());
        verifyNoInteractions(plugins);
    }
}
