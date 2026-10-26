package org.devlive.metaforge.common.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

public class NullAwareBeanUtils
{
    private NullAwareBeanUtils()
    {}

    /**
     * Filter the fields that are not empty in the target source, and copy the data source found in the database to the submitted target source
     *
     * @param source is the data source that is queried out from the database with the id
     * @param target submitted entity, target source
     */
    public static void copyNullProperties(Object source, Object target)
    {
        BeanUtils.copyProperties(source, target, getNoNullProperties(target));
    }

    /**
     * @param target source data
     * @return Retrieves the fields that are not empty in the target source
     */
    private static String[] getNoNullProperties(Object target)
    {
        BeanWrapper srcBean = new BeanWrapperImpl(target);
        PropertyDescriptor[] pds = srcBean.getPropertyDescriptors();
        Set<String> noEmptyName = new HashSet<>();
        for (PropertyDescriptor p : pds) {
            Object value = srcBean.getPropertyValue(p.getName());
            if (value != null) {
                noEmptyName.add(p.getName());
            }
        }
        String[] result = new String[noEmptyName.size()];
        return noEmptyName.toArray(result);
    }
}
