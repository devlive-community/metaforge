package org.devlive.metaforge.server.controller;

import org.devlive.metaforge.common.enums.ServiceState;
import org.devlive.metaforge.common.response.CommonResponse;
import org.devlive.metaforge.server.controller.admin.RoleController;
import org.devlive.metaforge.service.entity.RoleEntity;
import org.devlive.metaforge.service.repository.RoleRepository;
import org.devlive.metaforge.service.service.RoleService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class RoleControllerTest
{
    private final RoleRepository repository = mock(RoleRepository.class);
    private final RoleService service = mock(RoleService.class);
    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new RoleController(repository, service)).build();

    @Test
    public void postPreservesTheSpecifiedCode()
            throws Exception
    {
        when(service.createRole(any())).thenReturn(CommonResponse.success(new RoleEntity()));
        mvc.perform(post("/api/v1/role").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"CUSTOM_READ\",\"name\":\"Read role\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(true));
        ArgumentCaptor<RoleEntity> configure = ArgumentCaptor.forClass(RoleEntity.class);
        verify(service).createRole(configure.capture());
        assertEquals("CUSTOM_READ", configure.getValue().getCode());
    }

    @Test
    public void codeCheckUsesAStaticRouteAndReturnsAvailability()
            throws Exception
    {
        when(service.checkCode("CUSTOM_READ")).thenReturn(CommonResponse.success(false));
        mvc.perform(get("/api/v1/role/check-code").param("code", "CUSTOM_READ"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(false));
    }

    @Test
    public void pathUpdatesKeepTheOriginalResourceIdentity()
            throws Exception
    {
        when(service.updateRole(eq("ORIGINAL"), any())).thenReturn(CommonResponse.failure(ServiceState.USER_ROLE_CODE_IMMUTABLE));
        mvc.perform(put("/api/v1/role/ORIGINAL").contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"OTHER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(4030));
        verify(service).updateRole(eq("ORIGINAL"), any());
    }

    @Test
    public void legacyUpdatesUseTheStoredCodeForTheGivenId()
            throws Exception
    {
        RoleEntity original = new RoleEntity();
        original.setId(1L);
        original.setCode("ORIGINAL");
        when(repository.findById(1L)).thenReturn(Optional.of(original));
        when(service.updateRole(eq("ORIGINAL"), any())).thenReturn(CommonResponse.failure(ServiceState.USER_ROLE_CODE_IMMUTABLE));
        mvc.perform(put("/api/v1/role").contentType(MediaType.APPLICATION_JSON).content("{\"id\":1,\"code\":\"OTHER\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(4030));
    }

    @Test
    public void emptyMenuPutClearsAssignmentsInsteadOfCallingTheReadEndpoint()
            throws Exception
    {
        when(service.saveMenusByRoleId(1L, Set.of())).thenReturn(CommonResponse.success(true));
        mvc.perform(put("/api/v1/role/1/menus").contentType(MediaType.APPLICATION_JSON).content("[]"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data").value(true));
        verify(service).saveMenusByRoleId(1L, Set.of());
        verify(service, never()).getMenusByRoleId(any());
    }

    @Test
    public void menuGetUsesTheProjectionReadEndpoint()
            throws Exception
    {
        when(service.getMenusByRoleId(1L)).thenReturn(CommonResponse.success(List.of()));
        mvc.perform(get("/api/v1/role/1/menus")).andExpect(status().isOk()).andExpect(jsonPath("$.data").isArray());
        verify(service).getMenusByRoleId(1L);
    }

    @Test
    public void missingMenuPutBodyIsRejectedWithoutUpdatingAssignments()
            throws Exception
    {
        mvc.perform(put("/api/v1/role/1/menus").contentType(MediaType.APPLICATION_JSON)).andExpect(status().isBadRequest());
        verify(service, never()).saveMenusByRoleId(any(), any());
    }
}
