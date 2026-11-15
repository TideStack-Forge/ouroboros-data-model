package com.ouroboros.util;

import java.util.List;

public interface TreeNode<T extends TreeNode<T>> {
  List<T> getChildren();
}
