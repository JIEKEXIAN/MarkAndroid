package com.intlime.mark.tools.db;

import java.util.List;

/**
 * Created by root on 15-11-3.
 */
public interface DBInterface<T> {
    T get(int id);
    int delete(T bean);
    int insert(T bean);
    void insert(List<T> list);
    int update(T bean);
    void clear();
}
