package com.nordstrom.automation.selenium.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.SearchContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.internal.WrapsDriver;
import org.openqa.selenium.internal.WrapsElement;

import com.google.common.base.Throwables;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.SeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.core.WebDriverUtils;
import com.nordstrom.automation.selenium.support.SearchContextWait;
import com.thoughtworks.selenium.webdriven.commands.NoOp;

import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;

public abstract class ComponentContainer implements SearchContext, WrapsDriver, WrapsElement, CallbackFilter {
	
	protected WebDriver driver;
	protected SearchContext context;
	protected ComponentContainer parent;
	protected Method vacater;
	protected SearchContextWait wait;
	
	public static final By SELF = By.xpath(".");
	private static final List<String> METHODS;
	private static final List<Class<?>> BYPASS;
	private static final Class<?>[] ARG_TYPES = {SearchContext.class, ComponentContainer.class};
	
	static {
		METHODS = Arrays.asList("validateParent", "getDriver", "getContext", "getParent", "newChild", 
				"switchTo", "switchToContext", "getMethods", "accept", "getVacater", "isVacated");
		BYPASS = Arrays.asList(Object.class, WrapsDriver.class, WrapsElement.class, CallbackFilter.class);
	}
	
	/**
	 * Constructor for component container
	 * 
	 * @param context container search context
	 * @param parent container parent (may be {@code null} for {@link Page} objects
	 */
	public ComponentContainer(SearchContext context, ComponentContainer parent) {
		if (context == null) throw new IllegalArgumentException("Context must be non-null");
		validateParent(parent);
		
		this.context = context;
		this.driver = WebDriverUtils.getDriver(context);
		this.parent = parent;
	}
	
	/**
	 * Validate the specified parent object
	 * 
	 * @param parent container parent
	 */
	protected void validateParent(ComponentContainer parent) {
		if (parent == null) throw new IllegalArgumentException("Parent must be non-null");
	}

	/**
	 * Get the driver associated with this container
	 * 
	 * @return container driver
	 */
	public WebDriver getDriver() {
		return driver;
	}
	
	/**
	 * Get the container search context
	 * 
	 * @return container search context
	 */
	public SearchContext getContext() {
		return context;
	}
	
	/**
	 * Get the parent of this container
	 * 
	 * @return parent container
	 */
	public ComponentContainer getParent() {
		return parent;
	}
	
	/**
	 * 
	 * @return
	 */
	public Page getParentPage() {
		if (parent != null) return parent.getParentPage();
		return (Page) this;
	}
	
	/**
	 * 
	 * @return
	 */
	public SearchContextWait getWait() {
		if (wait == null) {
			SeleniumConfig config = SeleniumConfig.getConfig();
			long waitTimeout = config.getLong(SeleniumSettings.WAIT_TIMEOUT.key());
			wait = new SearchContextWait(this, waitTimeout);
		}
		return wait;
	}
	
	/**
	 * Switch driver to this container's search context.<br>
	 * <br>
	 * <b>NOTE</b>: This method walks down the container lineage to the parent page object, then back up to this 
	 * container, focusing the driver on each container as it goes.
	 * 
	 * @return driver focused on this container's context
	 */
	public WebDriver switchTo() {
		if (parent != null) parent.switchTo();
		return switchToContext();
	}
	
	/**
	 * Switch focus to this container's search context.<br>
	 * <br>
	 * <b>NOTE</b>: This protected method is used to focus the driver on this container's context. This is the worker 
	 * for the {@link #switchTo} method, and it must be called in proper sequence to work properly.
	 * 
	 * @return driver focused on this container's context
	 */
	protected abstract WebDriver switchToContext();
	
	/**
	 * 
	 * @return
	 */
	Method getVacater() {
		if (vacater != null) {
			return vacater;
		} else if (parent != null) {
			return parent.getVacater();
		} else {
			return null;
		}
	}
	
	/**
	 * 
	 * @param vacater
	 */
	void setVacater(Method vacater) {
		this.vacater = vacater;
		if (parent != null) parent.setVacater(vacater);
	}
	
	/**
	 * 
	 * @return
	 */
	boolean isVacated() {
		return (null != getVacater());
	}
	
	/**
	 * Create a container object of the specified class and context as a child of the target object
	 * 
	 * @param childClass class of child object to create
	 * @param context container search context
	 * @return new object of the specified type, with the current container as parent
	 */
	public <T extends ComponentContainer> T newChild(Class<T> childClass, SearchContext context) {
		return newChild(childClass, context, this);
	}
	
	/**
	 * Create a container object of the specified class and context as a child of the specified parent
	 * 
	 * @param childClass class of child object to create
	 * @param context container search context
	 * @param parent parent of the new container object
	 * @return new object of the specified type, with the specified container as parent
	 */
	public static <T extends ComponentContainer> T newChild(Class<T> childClass, SearchContext context, ComponentContainer parent) {
		T child = null;
		try {
			Constructor<T> ctor = childClass.getConstructor(SearchContext.class, ComponentContainer.class);
			child = ctor.newInstance(context, parent);
		} catch (InvocationTargetException e) {
			Throwables.propagate(e.getCause());
		} catch (SecurityException | IllegalAccessException | IllegalArgumentException e) {
			Throwables.propagate(e);
		} catch (NoSuchMethodException | InstantiationException e) {
			// never thrown because generic type is bounded
		}
		return child;
	}
	
	
	
	/**
	 * Find all elements within the current context using the given mechanism.
	 * 
	 * @param by the locating mechanism
	 * @return a list of all WebElements, or an empty list if nothing matches
	 */
	@Override
	public List<WebElement> findElements(By by) {
		return context.findElements(by);
	}
	
	/**
	 * Find the first WebElement using the given method.
	 * 
	 * @param by the locating mechanism
	 * @return the first matching element on the current context
	 */
	@Override
	public WebElement findElement(By by) {
		return context.findElement(by);
	}
	
	/**
	 * Get the driver object associated with this container.
	 * 
	 * @return container driver object
	 */
	@Override
	public WebDriver getWrappedDriver() {
		return driver;
	}
	
	/**
	 * Get the context element for this container.
	 * 
	 * @return container context element
	 */
	@Override
	public WebElement getWrappedElement() {
		return context.findElement(SELF);
	}
	
	/**
	 * 
	 * @param container
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T extends ComponentContainer> T enhanceContainer(T container) {
		Class<? extends ComponentContainer> type = container.getClass();
		if (Enhancer.isEnhanced(type)) return container;
		
		Enhancer enhancer = new Enhancer();
		enhancer.setSuperclass(type);
		enhancer.setCallbackTypes(new Class<?>[] {ContainerMethodInterceptor.class, NoOp.class});
		return (T) enhancer.create(ARG_TYPES, new Object[] {container.context, container.parent});
	}
	
	/**
	 * Map a method to a callback type.
	 * 
	 * @param method the intercepted method
	 * @return a callback type, as enumerated in the {@link Callbacks} interface
	 * @see Callbacks
	 */
	@Override
	public int accept(Method method) {
		if (BYPASS.contains(method.getDeclaringClass())) {
			return 1;
		} else if (METHODS.contains(method.getName())) {
			return 1;
		} else {
			return 0;
		}
	}
	
}
