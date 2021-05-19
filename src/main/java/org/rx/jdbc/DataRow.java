package org.rx.jdbc;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.rx.core.Reflects;
import org.rx.core.exception.InvalidException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.rx.core.App.require;

@RequiredArgsConstructor
public class DataRow implements Serializable {
    public static DataRow wrap(DataTable table, Object[] items) {
        DataRow row = new DataRow(table);
        row.setArray(items);
        return row;
    }

    @Getter
    private final DataTable table;
    final List<Object> items = new ArrayList<>();

    public Object[] getArray() {
        return items.toArray();
    }

    public void setArray(@NonNull Object[] array) {
        require(array, array.length == table.getColumns().size());

        items.clear();
        for (int i = 0; i < array.length; i++) {
            set(i, array[i]);
        }
    }

    public <T> T get(int ordinal) {
        return get(table.getColumn(ordinal));
    }

    public void set(int ordinal, Object item) {
        set(table.getColumn(ordinal), item);
    }

    public <T> T get(String columnName) {
        return get(table.getColumn(columnName));
    }

    public void set(String columnName, Object item) {
        set(table.getColumn(columnName), item);
    }

    public <T> T get(@NonNull DataColumn<T> column) {
        require(column, column.getTable() == table);

        return (T) items.get(column.ordinal);
    }

    public <T> void set(@NonNull DataColumn<T> column, T item) {
        require(column, column.getTable() == table);

        if (column.dataType != null && !Reflects.isInstance(item, column.dataType)) {
            throw new InvalidException("Item type error");
        }
        items.add(column.ordinal, item);
    }
}
