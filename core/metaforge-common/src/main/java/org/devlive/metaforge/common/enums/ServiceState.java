package org.devlive.metaforge.common.enums;

import lombok.Getter;

@Getter
public enum ServiceState
{
    SOURCE_NOT_FOUND(1001, "Source does not exist"),
    SOURCE_NOT_SUPPORTED(1002, "The current data source is not supported"),
    PLUGIN_NOT_FOUND(2001, "Plugin dose not exists"),
    PLUGIN_EXECUTE_FAILED(2002, "Plugin execute failed"),
    PLUGIN_ONLY_ONE_TEMPLATE(2003, "Plug-ins support only templates with the same name"),
    PLUGIN_CONFIGURE_MISMATCH(2004, "The plug-in passed parameters do not match the system configuration"),
    PLUGIN_CONFIGURE_REQUIRED(2005, "Ensure that all required fields exist"),
    REQUEST_VALID_ARGUMENT(3001, "The related parameters cannot be verified"),
    REQUEST_VALID_ARGUMENT_FORMAT(3002, "Unable to format related parameters"),
    REQUEST_VALID_ARGUMENT_LAYOUT(3003, "Related parameters cannot be resolved"),
    REQUEST_VALID_TYPE(3004, "Data types are not supported"),
    TEMPLATE_NOT_FOUND(3005, "Template dose not exists"),
    INVALID_REMOTE_ADDRESS(3006, "Invalid remote address"),
    USER_NOT_FOUND(4001, "User dose not exists"),
    USER_ROLE_NOT_FOUND(4002, "User role dose not exists"),
    USER_UNAUTHORIZED(4003, "Insufficient current user permissions"),
    USER_EXISTS(4004, "User exists"),
    USER_BAD_CREDENTIALS(4005, "The account or password is incorrect"),
    USER_PASSWORD_INCORRECT(4006, "The user password is incorrect"),
    USER_PASSWORD_DIFFERENT(4007, "Two passwords are different"),
    USER_NAME_EQUALS(4008, "Cannot be the same as the old user name"),
    USER_NAME_FORMAT_INVALID(4026, "Invalid username: 4-20 characters, letters, numbers and underscores, cannot start with a digit"),
    USER_NAME_ALREADY_EXISTS(4027, "The username already exists"),
    REQUEST_EXCEPTION(5000, "The request is abnormal"),
    INVALID_PARAMETER(5001, "Invalid parameter");

    private final Integer code;
    private final String value;

    ServiceState(Integer code, String value)
    {
        this.code = code;
        this.value = value;
    }
}
