/*
 * Copyright (C) 2014 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dagger;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Qualifier;
import javax.inject.Scope;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotates an interface or abstract class for which a fully-formed, dependency-injected
 * implementation is to be generated from a set of {@linkplain #modules}. The generated class will
 * have the name of the type annotated with {@code @Component} prepended with {@code Dagger}. For
 * example, {@code @Component interface MyComponent {...}} will produce an implementation named
 * {@code DaggerMyComponent}.
 *
 * <a name="component-methods">
 * <h2>Component methods</h2>
 * </a>
 *
 * <p>Every type annotated with {@code @Component} must contain at least one abstract component
 * method. Component methods may have any name, but must have signatures that conform to either
 * {@linkplain Provider provision} or {@linkplain MembersInjector members-injection} contracts.
 *
 * <a name="provision-methods">
 * <h3>Provision methods</h3>
 * </a>
 *
 * <p>Provision methods have no parameters and return an {@link Inject injected} or
 * {@link Provides provided} type. Each method may have a {@link Qualifier} annotation as well. The
 * following are all valid provision method declarations: <pre><code>
 *   SomeType getSomeType();
 *   {@literal Set<SomeType>} getSomeTypes();
 *   {@literal @PortNumber} int getPortNumber();
 * </code></pre>
 *
 * <p>Provision methods, like typical {@link Inject injection} sites, may use {@link Provider} or
 * {@link Lazy} to more explicitly control provision requests. A {@link Provider} allows the user
 * of the component to request provision any number of times by calling {@link Provider#get}. A
 * {@link Lazy} will only ever request a single provision, but will defer it until the first call to
 * {@link Lazy#get}. The following provision methods all request provision of the same type, but
 * each implies different semantics: <pre><code>
 *   SomeType getSomeType();
 *   {@literal Provider<SomeType>} getSomeTypeProvider();
 *   {@literal Lazy<SomeType>} getLazySomeType();
 * </code></pre>
 *
 * <a name="members-injection-methods">
 * <h3>Members-injection methods</h3>
 * </a>
 *
 * <p>Members-injection methods have a single parameter and inject dependencies into each of the
 * {@link Inject}-annotated fields and methods of the passed instance. A members-injection method
 * may be void or return its single parameter as a convenience for chaining. The following are all
 * valid members-injection method declarations: <pre><code>
 *   void injectSomeType(SomeType someType);
 *   SomeType injectAndReturnSomeType(SomeType someType);
 * </code></pre>
 *
 * <p>A method with no parameters that returns a {@link MembersInjector} is equivalent to a members
 * injection method. Calling {@link MembersInjector#injectMembers} on the returned object will
 * perform the same work as a members injection method. For example: <pre><code>
 *   {@literal MembersInjector<SomeType>} getSomeTypeMembersInjector();
 * </code></pre>
 *
 * <h4>A note about covariance</h4>
 *
 * <p>While a members-injection method for a type will accept instances of its subtypes, only
 * {@link Inject}-annotated members of the parameter type and its supertypes will be injected;
 * members of subtypes will not. For example, given the following types, only {@code a} and
 * {@code b} will be injected into an instance of {@code Child} when it is passed to the
 * members-injection method {@code injectSelf(Self instance)}: <pre><code>
 *   class Parent {
 *     @Inject A a;
 *   }
 *
 *   class Self extends Parent {
 *     @Inject B b;
 *   }
 *
 *   class Child extends Self {
 *     @Inject C c;
 *   }
 * </code></pre>
 *
 * <a name="instantiation">
 * <h2>Instantiation</h2>
 * </a>
 *
 * <p>Component implementations are primarily instantiated via a generated
 * <a href="http://en.wikipedia.org/wiki/Builder_pattern">builder</a>. An instance of the builder
 * is obtained using the {@code builder()} method on the component implementation. The returned
 * builder has a method to set each of the {@linkplain #modules} and component
 * {@linkplain #dependencies} named with the <a href="http://en.wikipedia.org/wiki/CamelCase">lower
 * camel case</a> version of the module or dependency type. Each component dependency and module
 * without a visible default constructor must be set explicitly, but any module with a default or
 * no-args constructor accessible to the component implementation may be elided. This is an example
 * usage of a component builder: <pre><code>
 *   public static void main(String[] args) {
 *     OtherComponent otherComponent = ...;
 *     MyComponent component = DaggerMyComponent.builder()
 *         // required because component dependencies must be set
 *         .otherComponent(otherComponent)
 *         // required because FlagsModule has constructor parameters
 *         .flagsModule(new FlagsModule(args))
 *         // may be elided because a no-args constructor is visible
 *         .myApplicationModule(new MyApplicationModule())
 *         .build();
 *   }
 * </code></pre>
 *
 * <p>In the case that a component has no component dependencies and only no-arg modules, the
 * generated component will also have a factory method {@code create()}.
 * {@code SomeComponent.create()} and {@code SomeComponent.builder().build()} are both valid and
 * equivalent.
 *
 * <a name="scope">
 * <h2>Scope</h2>
 * </a>
 *
 * <p>Each Dagger component can be associated with a scope by annotating it with the
 * {@linkplain Scope scope annotation}. The component implementation ensures that there is only one
 * provision of each scoped binding per instance of the component. If the component declares a
 * scope, it may only contain unscoped bindings or bindings of that scope anywhere in the graph. For
 * example: <pre><code>
 *   @Singleton @Component
 *   interface MyApplicationComponent {
 *     // this component can only inject types using unscoped or @Singleton bindings
 *   }
 * </code></pre>
 *
 * <p>In order to get the proper behavior associated with a scope annotation, it is the caller's
 * responsibility to instaniate new component instances when appropriate. A {@link Singleton}
 * component, for instance, should only be instantiated once per application, while a
 * {@code RequestScoped} component should be instantiated once per request. Because components are
 * self-contained implementations, exiting a scope is as simple as dropping all references to the
 * component instance.
 *
 * <a name="component-relationships">
 * <h2>Component relationships</h2>
 * </a>
 *
 * <p>While there is much utility in isolated components with purely unscoped bindings, many
 * applications will call for multiple components with multiple scopes to interact. Dagger provides
 * two mechanisms for relating components.
 *
 * <a name="subcomponents">
 * <h3>Subcomponents</h3>
 * </a>
 *
 * <p>The simplest way to relate two components is by declaring a {@link Subcomponent}. A
 * subcomponent behaves exactly like a component, but has its implementation generated within
 * a parent component or subcomponent. That relationship allows the subcomponent implementation to
 * inherit the <em>entire</em> binding graph from its parent when it is declared. For that reason,
 * a subcomponent isn't evaluated for completeness until it is associated with a parent.
 *
 * <p>Subcomponents are delared via a factory method on a parent component or subcomponent. The
 * method may have any name, but must return the subcomponent. The factory method's parameters may
 * be any number of the subcomponents's modules, but must at least include those without visible
 * no-arg constructors. The follwing is an example of a factory method that creates a request-scoped
 * subcomponent from a singleton-scoped parent: <pre><code>
 *   @Singleton @Component
 *   interface ApplicationComponent {
 *     // component methods...
 *
 *     RequestComponent newRequestComponent(RequestModule requestModule);
 *   }
 * </code></pre>
 *
 * <a name="component-dependencies">
 * <h3>Component dependencies</h3>
 * </a>
 *
 * <p>While subcomponents are the simplest way to compose subgraphs of bindings, subcomponents are
 * tightly coupled with the parents; they may use any binding defined by their ancestor component
 * and subcomponents. As an alternative, components can use bindings only from another
 * <em>component interface</em> by declaring a {@linkplain #dependencies component dependency}. When
 * a type is used as a component dependency, each <a href="#provision-methods">provision method</a>
 * on the dependency is bound as a provider. Note that <em>only</em> the bindings exposed as
 * provision methods are available through component dependencies.
 *
 * @author Gregory Kick
 * @since 2.0
 */
@Retention(RUNTIME) // Allows runtimes to have specialized behavior interoperating with Dagger.
@Target(TYPE)
@Documented
public @interface Component {
  /**
   * A list of classes annotated with {@link Module} whose bindings are used to generate the
   * component implementation. Note that through the use of {@link Module#includes} the full set of
   * modules used to implement the component may include more modules that just those listed here.
   */
  Class<?>[] modules() default {};

  /**
   * A list of types that are to be used as <a href="#component-dependencies">component
   * dependencies</a>.
   */
  Class<?>[] dependencies() default {};
}
