package com.ouroboros.data.record;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.ouroboros.data.util.DataJson;

@SuppressWarnings("deprecation")
public class DefaultRecordListImpl implements RecordList {

  final List<Map<String, ?>> records;

  @SuppressWarnings("unchecked")
  public DefaultRecordListImpl(Collection<? extends Map<String, ?>> c) {
    super();
    if (c instanceof List<?> list) {
      records = (List<Map<String, ?>>) list;
    } else {
      records = new ArrayList<>(c);
    }
  }

  @Override
  public int size() {
    return records.size();
  }

  @Override
  public boolean isEmpty() {
    return records.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return records.contains(o);
  }

  @Override
  public Iterator<Record> iterator() {
    var iterator = records.iterator();
    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        return iterator.hasNext();
      }

      @Override
      public Record next() {
        return Record.of(iterator.next());
      }
    };
  }

  @Override
  public Object[] toArray() {
    return records.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return records.toArray(a);
  }

  @Override
  public boolean add(Record record) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public boolean remove(Object o) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return records.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends Record> c) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public boolean addAll(int index, Collection<? extends Record> c) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public void clear() {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public Record get(int index) {
    return Record.of(records.get(index));
  }

  @Override
  public Record set(int index, Record element) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public void add(int index, Record element) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public Record remove(int index) {
    throw new UnsupportedOperationException("RecordList 不支持修改操作");
  }

  @Override
  public int indexOf(Object o) {
    return records.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return records.lastIndexOf(o);
  }

  @Override
  public ListIterator<Record> listIterator() {
    var listIterator = records.listIterator();
    return new ListIterator<>() {
      @Override
      public boolean hasNext() {
        return listIterator.hasNext();
      }

      @Override
      public Record next() {
        return Record.of(listIterator.next());
      }

      @Override
      public boolean hasPrevious() {
        return listIterator.hasPrevious();
      }

      @Override
      public Record previous() {
        return Record.of(listIterator.previous());
      }

      @Override
      public int nextIndex() {
        return listIterator.nextIndex();
      }

      @Override
      public int previousIndex() {
        return listIterator.previousIndex();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("RecordList 不支持修改操作");
      }

      @Override
      public void set(Record e) {
        throw new UnsupportedOperationException("RecordList 不支持修改操作");
      }

      @Override
      public void add(Record e) {
        throw new UnsupportedOperationException("RecordList 不支持修改操作");
      }
    };
  }

  @Override
  public ListIterator<Record> listIterator(int index) {
    var listIterator = records.listIterator(index);
    return new ListIterator<>() {
      @Override
      public boolean hasNext() {
        return listIterator.hasNext();
      }

      @Override
      public Record next() {
        return Record.of(listIterator.next());
      }

      @Override
      public boolean hasPrevious() {
        return listIterator.hasPrevious();
      }

      @Override
      public Record previous() {
        return Record.of(listIterator.previous());
      }

      @Override
      public int nextIndex() {
        return listIterator.nextIndex();
      }

      @Override
      public int previousIndex() {
        return listIterator.previousIndex();
      }

      @Override
      public void remove() {
        throw new UnsupportedOperationException("RecordList 不支持修改操作");
      }

      @Override
      public void set(Record e) {
        throw new UnsupportedOperationException("RecordList 不支持修改操作");
      }

      @Override
      public void add(Record e) {
        throw new UnsupportedOperationException("RecordList 不支持修改操作");
      }
    };
  }

  @Override
  public List<Record> subList(int fromIndex, int toIndex) {
    return records.subList(fromIndex, toIndex).stream()
        .map(Record::of)
        .collect(Collectors.toList());
  }

  public void mutateRecord(Consumer<Map<String, Object>> recordModifier) {
    var iterator = records.listIterator();
    while (iterator.hasNext()) {
      var record = iterator.next();
      if (record instanceof DefaultRecordImpl defaultRecord) {
        defaultRecord.mutate(recordModifier);
      }
      if (record instanceof HashMap<?, ?>
          || record instanceof TreeMap<?, ?>) {
        recordModifier.accept((Map<String, Object>) record);
      } else {
        Map<String, Object> newRecord = new LinkedHashMap<>(record);
        recordModifier.accept(newRecord);
        iterator.set(newRecord);
      }
    }
  }

  @Override
  public String toString() {
    return DataJson.toJsonString(records);
  }
}
