package com.ouroboros.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import com.ouroboros.util.map.ImmutableHierarchicalMap;
import com.ouroboros.util.map.MutableHierarchicalMap;
import com.ouroboros.util.map.Model;
import com.ouroboros.util.map.ModelValue;
import com.ouroboros.util.map.Pack;

public class MapsCoverageTest {

  @Test
  public void coversGeneralMapUtilities() {
    Map<String, Object> base = new LinkedHashMap<String, Object>();
    base.put("a", 1);
    base.put("b", 2);

    assertTrue(Maps.isStringKeyMap(Collections.singletonMap("x", 1)));
    assertFalse(Maps.isStringKeyMap(Collections.emptyMap()));
    assertTrue(Maps.keyExistsPredicate("a").test(base));
    assertTrue(Maps.containsKey(base, "a"));
    assertFalse(Maps.isEmpty(base));
    assertTrue(Maps.isNotEmpty(base));

    assertEquals(Collections.singletonMap("A", 1), Maps.<String, String, Integer>remap(Collections.singletonMap("a", 1), (String key) -> key.toUpperCase()));
    assertEquals(Collections.singletonMap("a_1", 1), Maps.remap(Collections.singletonMap("a", 1), (key, value) -> key + "_" + value));
    assertEquals(Collections.singletonMap("A", "1"), Maps.remap(Collections.singletonMap("a", 1), String::toUpperCase, String::valueOf));
    assertEquals(Collections.singletonMap("a1", "a=1"), Maps.remap(Collections.singletonMap("a", 1), (key, value) -> key + value, (key, value) -> key + "=" + value));

    Map<String, Integer> merged = Maps.merge((left, right) -> left + right, Collections.<String, Integer>singletonMap("a", 1), Collections.<String, Integer>singletonMap("b", 3), Collections.<String, Integer>singletonMap("b", 2), Collections.<String, Integer>singletonMap("c", 4));
    assertEquals(Integer.valueOf(5), merged.get("b"));
    assertEquals(Integer.valueOf(4), merged.get("c"));
    assertEquals(Integer.valueOf(2), Maps.merge((key, left, right) -> left * right, Collections.singletonMap("x", 1), Collections.singletonMap("x", 2)).get("x"));

    Map<String, Object> deep = Maps.deepMerge(
        Collections.<String, Object>singletonMap("user", Collections.<String, Object>singletonMap("name", "neo")),
        Collections.<String, Object>singletonMap("user", Collections.<String, Object>singletonMap("age", 18))
    );
    assertEquals("neo", ((Map<?, ?>) deep.get("user")).get("name"));
    assertEquals(18, ((Map<?, ?>) deep.get("user")).get("age"));

    Map<String, Object> nested = new LinkedHashMap<String, Object>();
    nested.put("list", new ArrayList<Map<String, Object>>(Collections.<Map<String, Object>>singletonList(Collections.<String, Object>singletonMap("x", 1))));
    nested.put("map", Collections.<String, Object>singletonMap("y", 2));
    Map<String, Object> clone = Maps.deepClone(nested, LinkedHashMap::new);
    assertEquals(nested, clone);
    assertFalse(nested == clone);
    assertFalse(nested.get("list") == clone.get("list"));
    assertTrue(Maps.deepCloneUnmodifiable(nested).containsKey("map"));

    assertEquals(Collections.singletonMap("b", 2), Maps.pick(base, key -> key.equals("b")));
    assertEquals(Collections.singletonMap("b", 2), Maps.pick(base, Arrays.asList("b")));
    assertEquals(Collections.singletonMap("a", 1), Maps.pick(base, (key, value) -> ((Integer) value) < 2));
    assertEquals(Collections.singletonMap("a", 1), Maps.omit(base, Collections.singleton("b")));
    assertEquals(Collections.singletonMap("b", 2), Maps.omit(base, (key, value) -> ((Integer) value) < 2));

    Map<String, Object> mutable = new LinkedHashMap<String, Object>(base);
    assertEquals(Collections.singletonMap("a", 1), Maps.removeExcept(mutable, "a"));
    mutable = new LinkedHashMap<>(base);
    assertEquals(Collections.singletonMap("b", 2), Maps.remove(mutable, Arrays.asList("a")));
    mutable = new LinkedHashMap<>(base);
    Maps.putIfAbsentAll(mutable, Collections.singletonMap("a", 9));
    Maps.putIfAbsentAll(mutable, Collections.singletonMap("c", 3));
    Maps.putIf(mutable, "d", 4, value -> ((Integer) value) > 3);
    assertEquals(Integer.valueOf(3), mutable.get("c"));
    assertEquals(Integer.valueOf(4), mutable.get("d"));

    assertEquals(Optional.of(1), Maps.getAs(base, "a", Integer.class));
    assertEquals(Integer.valueOf(5), Maps.getAsOrDefault(base, "missing", Integer.class, 5));
    assertEquals(Integer.valueOf(0), Maps.getAsOrDefault(Collections.singletonMap("n", 6), "missing", Integer.class, 0));
    assertEquals(Collections.singletonMap("1", "10"), Maps.transform(Collections.singletonMap(1, 10), String::valueOf, String::valueOf));
    assertEquals(Collections.singletonMap("b", 2), Maps.intersection(base, Collections.singletonMap("b", 2)));
    assertEquals(Collections.singletonMap("a", 1), Maps.difference(base, Collections.singletonMap("b", 2)));
    assertEquals(Optional.of(2), Maps.firstNonNull(new LinkedHashMap<String, Integer>() {{ put("a", null); put("b", 2); }}, "a", "b"));
    assertNull(Maps.firstMapped(Collections.singletonMap("a", null), "a", "b"));

    assertEquals(Collections.emptyMap(), Maps.pick(base, (String[]) null));
    assertEquals(base, Maps.omit(base, (String[]) null));
    assertEquals(Collections.emptyMap(), Maps.transform(null, String::valueOf, String::valueOf));
    assertEquals(Collections.emptyMap(), Maps.intersection(null, base));
    assertEquals(base, Maps.difference(base, null));

    Map<String, Object> deepTarget = new LinkedHashMap<String, Object>();
    deepTarget.put("user", Collections.<String, Object>singletonMap("name", "neo"));
    Maps.deepMergeTo(deepTarget, Collections.<String, Object>singletonMap("user", Collections.<String, Object>singletonMap("age", 18)));
    assertEquals(18, ((Map<?, ?>) deepTarget.get("user")).get("age"));
    assertTrue(Maps.deepClone(null).isEmpty());
    assertTrue(Maps.deepClone(null, LinkedHashMap::new).isEmpty());
    try {
      Maps.deepCloneUnmodifiable(Collections.singletonMap("a", 1)).put("b", 2);
      fail("expected unmodifiable map");
    } catch (UnsupportedOperationException expected) {
    }

    Pack pack = Maps.toBean(Pack.class, Collections.<String, Object>singletonMap("a", Collections.<String, Object>singletonMap("id", "42")));
    assertEquals("42", ((Model) pack.getA().get()).getId());
    assertTrue(Maps.tryToBean(Pack.class, Collections.singletonMap("a", "43")).isSuccess());
    assertEquals("9", Maps.fromBean(new Pack(ModelValue.primary("9"))).get("a"));
    assertNotEquals(Collections.emptyMap(), Maps.fromBean(new Pack(ModelValue.model(new Model("10")))));
  }

  @Test
  public void fromBeanStopsAtCyclicReference() {
    ParentBean parent = new ParentBean("xxx");
    ChildBean child = new ChildBean("xxx01");
    child.setParent(parent);
    parent.setChildren(Collections.singletonList(child));

    Map<String, Object> result = Maps.fromBean(parent);

    assertEquals("xxx", result.get("parentName"));
    List<?> children = (List<?>) result.get("children");
    assertEquals(1, children.size());
    Map<?, ?> childMap = (Map<?, ?>) children.get(0);
    assertEquals("xxx01", childMap.get("childName"));
    assertTrue(childMap.containsKey("parent"));
    assertNull(childMap.get("parent"));
  }

  @Test
  public void fromBeanConvertsNestedCollectionsAndMaps() {
    ReadOnlyContainerBean bean = new ReadOnlyContainerBean("root");
    bean.setChildren(Collections.singletonList(new ReadOnlyValueBean("child")));
    bean.setAttributes(Collections.<String, Object>singletonMap("primary", new ReadOnlyValueBean("mapped")));

    Map<String, Object> result = Maps.fromBean(bean);

    assertEquals("root", result.get("name"));
    List<?> children = (List<?>) result.get("children");
    assertEquals("child", ((Map<?, ?>) children.get(0)).get("name"));
    Map<?, ?> attributes = (Map<?, ?>) result.get("attributes");
    assertEquals("mapped", ((Map<?, ?>) attributes.get("primary")).get("name"));
  }

  @Test
  public void convertsNestedMapsCollectionsAndMapValuesToBean() {
    Map<String, Object> child = new LinkedHashMap<String, Object>();
    child.put("name", "child");
    child.put("count", "7");
    child.put("active", "true");
    Map<String, Object> mapChild = Collections.<String, Object>singletonMap("name", "mapped");
    Map<String, Object> nestedMetadata = Collections.<String, Object>singletonMap("raw", "value");

    Map<String, Object> source = new LinkedHashMap<String, Object>();
    source.put("name", "root");
    source.put("child", child);
    source.put("children", Collections.singletonList(child));
    source.put("childrenByKey", Collections.<String, Object>singletonMap("primary", mapChild));
    source.put("metadata", Collections.<String, Object>singletonMap("nested", nestedMetadata));
    source.put("nullable", null);

    WritableContainerBean bean = Maps.toBean(WritableContainerBean.class, source);

    assertEquals("root", bean.getName());
    assertEquals("child", bean.getChild().getName());
    assertEquals(Integer.valueOf(7), bean.getChild().getCount());
    assertEquals(Boolean.TRUE, bean.getChild().getActive());
    assertEquals("child", bean.getChildren().get(0).getName());
    assertEquals("mapped", bean.getChildrenByKey().get("primary").getName());
    assertEquals(nestedMetadata, bean.getMetadata().get("nested"));
    assertNull(bean.getNullable());
    assertEquals("default", bean.getDefaultValue());
  }

  @Test
  public void convertsEnumUsingValueFactory() {
    EnumHolder holder = Maps.toBean(EnumHolder.class, Collections.<String, Object>singletonMap("scope", "path"));

    assertEquals(RequestScope.PATH, holder.getScope());
  }

  @Test
  public void preservesJsonBooleanIsPropertyNames() {
    BooleanIsBean bean = Maps.toBean(BooleanIsBean.class, Collections.<String, Object>singletonMap("isDeleted", true));
    BooleanIsBean aliasBean = Maps.toBean(BooleanIsBean.class, Collections.<String, Object>singletonMap("deleted", true));

    assertTrue(bean.isDeleted());
    assertTrue(aliasBean.isDeleted());

    Map<String, Object> map = Maps.fromBean(bean);
    assertTrue(map.containsKey("isDeleted"));
    assertFalse(map.containsKey("deleted"));
    assertEquals(Boolean.TRUE, map.get("isDeleted"));
  }

  @Test
  public void convertsInheritedGenericBeanProperties() {
    Map<String, Object> value = Collections.<String, Object>singletonMap("name", "nested");
    GenericHolder holder = Maps.toBean(GenericHolder.class, Collections.<String, Object>singletonMap("value", value));

    assertEquals("nested", holder.getValue().getName());
  }

  @Test
  public void convertsBeanPropertiesWithFluentSetters() {
    Map<String, Object> source = new LinkedHashMap<String, Object>();
    source.put("name", "demo");
    source.put("count", "7");

    FluentSetterBean bean = Maps.toBean(FluentSetterBean.class, source);

    assertEquals("demo", bean.getName());
    assertEquals(Integer.valueOf(7), bean.getCount());
  }

  @Test
  public void coversHierarchicalOperationsAndFacades() {
    Map<String, Object> flat = new LinkedHashMap<String, Object>();
    flat.put("user.name", "Neo");
    flat.put("user.tags[0]", "chosen");
    flat.put("user.tags[1]", "one");
    flat.put("meta.active", true);

    Map<String, Object> built = Maps.Hierarchical.build(flat);
    assertEquals("Neo", ((Map<?, ?>) built.get("user")).get("name"));
    assertEquals("chosen", ((List<?>) ((Map<?, ?>) built.get("user")).get("tags")).get(0));
    assertEquals(flat, Maps.Hierarchical.flatten(built));
    assertEquals(Collections.singletonMap("user.tags", Arrays.asList("chosen", "one")), Maps.Hierarchical.flattenMapsOnly(Collections.singletonMap("user", Collections.singletonMap("tags", Arrays.asList("chosen", "one")))));

    Map<String, Object> target = new LinkedHashMap<String, Object>();
    Maps.Hierarchical.setValue(target, "app.items[0].name", "alpha");
    Maps.Hierarchical.setValue(target, Arrays.asList("app", "items", "1", "name"), "beta");
    assertEquals("alpha", Maps.Hierarchical.getValue(target, "app.items[0].name").orElse(null));
    assertEquals("beta", Maps.Hierarchical.getValue(target, Arrays.asList("app", "items", "1", "name")).orElse(null));
    assertEquals(Arrays.asList("a", "0", "b"), Maps.Hierarchical.defaultPathSplitter("a[0].b"));

    MutableHierarchicalMap mutable = Maps.newHierarchical();
    mutable.setValue("user.name", "Trinity").setValue("user.roles[0]", "operator");
    assertTrue(mutable.hasPath("user.name"));
    assertEquals(Optional.of("Trinity"), mutable.getValue("user.name", String.class));
    assertEquals(Optional.of("operator"), mutable.removePath("user.roles[0]"));
    mutable.flatten().build();
    ImmutableHierarchicalMap immutable = mutable.asImmutable();
    assertTrue(immutable.hasPath("user.name"));
    assertEquals("Trinity", immutable.getValue("user.name").orElse(null));
    assertTrue(immutable.getWrappedMap().containsKey("user"));
    assertFalse(immutable.toMutableShallow().flatten().getWrappedMap().isEmpty());
    assertFalse(immutable.toMutable().flatten().getWrappedMap().isEmpty());
    assertTrue(immutable.toString().contains("ImmutableHierarchicalMap"));
    assertTrue(mutable.toString().contains("MutableHierarchicalMap"));

    var builtMap = Maps.<String, Integer>builder().put("x", 1).putAll(Collections.singletonMap("y", 2)).build();
    assertEquals(Integer.valueOf(1), builtMap.get("x"));
    assertEquals(Integer.valueOf(2), builtMap.get("y"));
    assertTrue(Maps.hierarchical(new LinkedHashMap<>()).getWrappedMap().isEmpty());
    assertTrue(Maps.immutableHierarchical(built).build().containsKey("user"));

    assertTrue(Maps.Hierarchical.build(null, path -> Arrays.asList(path.split("/"))).isEmpty());
    assertEquals("Neo", ((Map<?, ?>) Maps.Hierarchical.build(Collections.singletonMap("user/name", "Neo"), path -> Arrays.asList(path.split("/"))).get("user")).get("name"));
    assertEquals(Optional.empty(), Maps.Hierarchical.getValue(null, "user.name"));
    assertEquals(Optional.empty(), Maps.Hierarchical.getValue(target, (List<String>) null));
    assertEquals(Collections.emptyMap(), Maps.Hierarchical.flatten(null));
    assertEquals(Collections.emptyMap(), Maps.Hierarchical.flattenMapsOnly(null));
  }

  private static final class ParentBean {
    private final String parentName;
    private List<ChildBean> children = Collections.emptyList();

    private ParentBean(String parentName) {
      this.parentName = parentName;
    }

    public String getParentName() {
      return parentName;
    }

    public List<ChildBean> getChildren() {
      return children;
    }

    public void setChildren(List<ChildBean> children) {
      this.children = children;
    }
  }

  private static final class ChildBean {
    private final String childName;
    private ParentBean parent;

    private ChildBean(String childName) {
      this.childName = childName;
    }

    public String getChildName() {
      return childName;
    }

    public ParentBean getParent() {
      return parent;
    }

    public void setParent(ParentBean parent) {
      this.parent = parent;
    }
  }

  private static final class ReadOnlyContainerBean {
    private final String name;
    private List<ReadOnlyValueBean> children = Collections.emptyList();
    private Map<String, Object> attributes = Collections.emptyMap();

    private ReadOnlyContainerBean(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public List<ReadOnlyValueBean> getChildren() {
      return children;
    }

    public void setChildren(List<ReadOnlyValueBean> children) {
      this.children = children;
    }

    public Map<String, Object> getAttributes() {
      return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
      this.attributes = attributes;
    }
  }

  private static final class ReadOnlyValueBean {
    private final String name;

    private ReadOnlyValueBean(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }

  private static final class WritableContainerBean {
    private String name;
    private WritableValueBean child;
    private List<WritableValueBean> children = Collections.emptyList();
    private Map<String, WritableValueBean> childrenByKey = Collections.emptyMap();
    private Map<String, Object> metadata = Collections.emptyMap();
    private String nullable = "not-null";
    private String defaultValue = "default";

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public WritableValueBean getChild() {
      return child;
    }

    public void setChild(WritableValueBean child) {
      this.child = child;
    }

    public List<WritableValueBean> getChildren() {
      return children;
    }

    public void setChildren(List<WritableValueBean> children) {
      this.children = children;
    }

    public Map<String, WritableValueBean> getChildrenByKey() {
      return childrenByKey;
    }

    public void setChildrenByKey(Map<String, WritableValueBean> childrenByKey) {
      this.childrenByKey = childrenByKey;
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
      this.metadata = metadata;
    }

    public String getNullable() {
      return nullable;
    }

    public void setNullable(String nullable) {
      this.nullable = nullable;
    }

    public String getDefaultValue() {
      return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
      this.defaultValue = defaultValue;
    }
  }

  private static final class WritableValueBean {
    private String name;
    private Integer count;
    private Boolean active;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Integer getCount() {
      return count;
    }

    public void setCount(Integer count) {
      this.count = count;
    }

    public Boolean getActive() {
      return active;
    }

    public void setActive(Boolean active) {
      this.active = active;
    }
  }

  private static final class FluentSetterBean {
    private String name;
    private Integer count;

    public String getName() {
      return name;
    }

    public FluentSetterBean setName(String name) {
      this.name = name;
      return this;
    }

    public Integer getCount() {
      return count;
    }

    public FluentSetterBean setCount(Integer count) {
      this.count = count;
      return this;
    }
  }

  private enum RequestScope {
    PATH("path"),
    QUERY("query");

    private final String value;

    RequestScope(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    public static RequestScope fromValue(String value) {
      for (RequestScope scope : values()) {
        if (scope.value.equalsIgnoreCase(value)) {
          return scope;
        }
      }
      return null;
    }
  }

  private static final class EnumHolder {
    private RequestScope scope;

    public RequestScope getScope() {
      return scope;
    }

    public void setScope(RequestScope scope) {
      this.scope = scope;
    }
  }

  private static final class BooleanIsBean {
    @JsonProperty("isDeleted")
    private boolean isDeleted;

    @JsonProperty("isDeleted")
    public boolean isDeleted() {
      return isDeleted;
    }

    public void setDeleted(boolean deleted) {
      isDeleted = deleted;
    }
  }

  private static class GenericBase<T> {
    private T value;

    public T getValue() {
      return value;
    }

    public void setValue(T value) {
      this.value = value;
    }
  }

  private static final class GenericHolder extends GenericBase<GenericItem> {
  }

  private static final class GenericItem {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }
}
