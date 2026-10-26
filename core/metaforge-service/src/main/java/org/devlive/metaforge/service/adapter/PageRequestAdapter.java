package org.devlive.metaforge.service.adapter;

import org.apache.commons.lang3.ObjectUtils;
import org.devlive.metaforge.common.utils.CollectorUtils;
import org.devlive.metaforge.service.body.FilterBody;
import org.devlive.metaforge.service.body.OrderBody;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class PageRequestAdapter
{
    private PageRequestAdapter()
    {
    }

    public static PageRequest of(int start, int end)
    {
        return PageRequest.of(start - 1, end);
    }

    public static PageRequest of(int start, int end, Sort sort)
    {
        return PageRequest.of(start - 1, end, sort);
    }

    public static PageRequest of(FilterBody filter)
    {
        PageRequest pageable = PageRequestAdapter.of(filter.getPage(), filter.getSize());
        if (ObjectUtils.isNotEmpty(filter.getOrders())) {
            // Remove duplicate data passed
            List<OrderBody> remoted = new ArrayList<>();
            filter.getOrders()
                    .stream()
                    .filter(CollectorUtils.distinctByKey(OrderBody::getColumn))  //filter保留true的值
                    .forEach(remoted::add);

            List<Sort.Order> orders = new ArrayList<>();
            remoted.stream()
                    .filter(value -> value.getOrder().equalsIgnoreCase("asc") || value.getOrder().equalsIgnoreCase("desc")) // Filter other
                    .forEach(value -> {
                        if (value.getOrder().equalsIgnoreCase("desc")) {
                            orders.add(Sort.Order.desc(value.getColumn()));
                        }
                        else {
                            orders.add(Sort.Order.asc(value.getColumn()));
                        }
                    });
            Sort sort = Sort.by(orders);
            pageable = PageRequestAdapter.of(filter.getPage(), filter.getSize(), sort);
        }
        return pageable;
    }
}
