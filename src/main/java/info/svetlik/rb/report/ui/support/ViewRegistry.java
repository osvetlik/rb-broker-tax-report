package info.svetlik.rb.report.ui.support;

public interface ViewRegistry {

	<T> View<T> view(String name, Class<T> controllerClass);
	String css(String name);

}
