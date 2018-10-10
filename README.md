# magnifierFx
magnifier for javafx

![sample](http://pc3aukg6f.bkt.clouddn.com/blog/29otc.gif)

javafx版本的放大镜，超好用！

### usage

```java
magnifier = new Magnifier();
magnifier.setActive(true);
magnifier.setRadius(60);
magnifier.setFrameWidth(4);
magnifier.setScaleFactor(2);
magnifier.setScopeLineWidth(1.5);
magnifier.setScopeLinesVisible(true);
magnifier.setContent(imageView);

container.getChildren().add(magnifier);
```

