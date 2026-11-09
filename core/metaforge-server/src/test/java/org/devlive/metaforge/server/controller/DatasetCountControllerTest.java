package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.plugin.PluginManager;
import org.devlive.metaforge.server.support.PersistenceContext;
import org.devlive.metaforge.service.repository.DataSetColumnRepository;
import org.devlive.metaforge.service.repository.DataSetRepository;
import org.devlive.metaforge.service.security.UserDetailsService;
import org.devlive.metaforge.service.service.FunctionService;
import org.devlive.metaforge.service.service.impl.DataSetServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Counts real owned metadata rows without loading legacy identities, states or source references. */
public class DatasetCountControllerTest
{
    private static PersistenceContext context;
    private final PluginManager plugins = mock(PluginManager.class);
    private MockMvc mvc;

    @BeforeAll
    public static void start() throws Exception
    {
        context = PersistenceContext.start("h2");
    }

    @AfterAll
    public static void stop()
    {
        context.close();
    }

    @BeforeEach
    public void setup()
    {
        context.jdbc().update("DELETE FROM metaforge_dataset");
        // One missing code and an unreadable legacy state must not hide the correct count.
        context.jdbc().update("INSERT INTO metaforge_dataset (name, code, state, source_id, user_id, active) VALUES "
                + "('one', 'ONE', 'METADATA_SUCCESS', 999, 11, TRUE), "
                + "('legacy', NULL, 'UNKNOWN_LEGACY_STATE', 999, 11, TRUE), "
                + "('inactive', 'THREE', NULL, NULL, 11, FALSE), "
                + "('other', 'OTHER', NULL, NULL, 22, TRUE), "
                + "('unowned', 'UNOWNED', NULL, NULL, NULL, TRUE)");
        DataSetRepository repository = context.bean(DataSetRepository.class);
        DataSetServiceImpl service = new DataSetServiceImpl(repository, mock(DataSetColumnRepository.class), null, plugins,
                null, null, null, null, null, null, null, null);
        mvc = MockMvcBuilders.standaloneSetup(new DataSetController(repository, service, mock(FunctionService.class), null)).build();
        signIn(11L);
    }

    private static void signIn(Long id)
    {
        UserDetailsService user = new UserDetailsService(id, "USER", "owner", "", List.of(), null);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities()));
    }

    @AfterEach
    public void clear()
    {
        SecurityContextHolder.clearContext();
    }

    @Test
    public void countsOwnedRowsIncludingMissingCodesWithoutReadingTheStoragePlugin() throws Exception
    {
        mvc.perform(get("/api/v1/dataset/count").param("userId", "22"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true)).andExpect(jsonPath("$.data").value(3));
        verifyNoInteractions(plugins);
    }

    @Test
    public void changesScopeWithTheAuthenticatedUserAndPreservesRealZero() throws Exception
    {
        signIn(22L);
        mvc.perform(get("/api/v1/dataset/count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true)).andExpect(jsonPath("$.data").value(1));
        signIn(99L);
        mvc.perform(get("/api/v1/dataset/count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true)).andExpect(jsonPath("$.data").value(0));
    }

    @Test
    public void missingAuthenticationNeverCountsAllUsers() throws Exception
    {
        SecurityContextHolder.clearContext();
        mvc.perform(get("/api/v1/dataset/count"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(false));
    }
}
