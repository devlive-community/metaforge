package org.devlive.metaforge.service.validation;

import javax.validation.groups.Default;

public interface ValidationGroup
        extends Default
{
    interface Crud
            extends ValidationGroup
    {
        interface Create
                extends Crud {}

        interface Update
                extends Crud {}

        interface Query
                extends Crud {}

        interface Delete
                extends Crud {}

        interface Auth
                extends Crud {}
    }
}
