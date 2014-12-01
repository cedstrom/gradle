This spec outlines some initial stories for what's generally termed “managed model”.
This is a sub stream of the “unified-configuration-model” stream.
   
## Background

There are several key drivers for this stream of work:

1. Model insight
2. Bidirectional model externalisation
3. Model caching

The term “model insight” (#1) refers to deeply understanding the structure of model elements.
While not a goal with immediate user benefit in itself, it is included in this list as a proxy for general usability enhancements made possible by this.
For example, a model browsing tool would rely on this insight to generate visual representation of elements and facilitate browsing a model (schema and data).

The term “bidirectional model externalisation” refers to being able to serialize a data set to some “external” form (e.g. JSON, YAML, XML, Turtle etc.) for consumption by other systems, and the possibility of using such a format to construct a data set for Gradle to use.

The term “model caching” refers to the ability to safely reuse a previously “built” model element, avoiding the need to execute the user code that contributed to its final state. 

Moreover, we consider owning the implementation of model elements an enabler for the general dependency based configuration model.
   
## Stories
                                            
### ~~Plugin creates model element of custom, simple, type without supplying an implementation~~
                                            
This story makes the following possible…

    @Managed
    interface Person {
        String getName();
        void setName(String name);
    }
    
    class RulePlugin {
        @Model
        void createPerson(Person person) {
          person.setName("foo")
        }
        
        @Mutate
        void addPersonTask(CollectionBuilder<Task> tasks, Person person) {
            tasks.create("echo", t -> 
              t.doLast(t2 -> System.out.println(person.getName())); // prints 'foo'
            );
        }
    }
    
1. No implementation of `Person` is provided
2. A `@Model` method returning `void` indicates the the first arg should be an “empty” instance of the model type and is the thing to be created (all other args are inputs to the rule)
3. Only support for `String` properties is required at this point - it is an error to have a property of any other type
4. Properties conform to the JavaBean convention - it is an error to have a method that doesn't conform to this, or a non read & write property
4. `@Managed` types must be interfaces and cannot extend other interfaces
5. Validation occurs early (when rule is encountered, i.e. before it is executed)

> Note: most of those constraints are just temporary and will be loosened by future stories

#### Test Coverage

- ~~(something like snippet above)~~
- ~~(constraints mentioned above cause errors when violated, error message points to “what” caused the type to be considered)~~
- ~~`void` returning `@Model` method with non `@Managed` type as first arg causes error~~

#### Open questions

1. Should we require that types that are designed to be managed model elements be annotated?
2. Do we need to consider non identity based equals/hashCode at this time?

### ~~Plugin creates model element of custom, composite, type without supplying an implementation~~

    @Managed
    interface Platform {
        String getDisplayName();
        void setDisplayName(String name);
        OperatingSystem getOperatingSystem();
    }
    
    @Managed 
    interface OperatingSystem {
        String getName();
        void setName(String name);
    }
    
    class RulePlugin {
        @Model
        void createPlatform(Platform platform) {
          platform.setDisplayName("Microsoft Windows")
          platform.getOperatingSystem().setName("windows")
        }
        
        @Mutate
        void addPersonTask(CollectionBuilder<Task> tasks, Platform platform) {
            tasks.create("echo", t -> 
              t.doLast(t2 -> System.out.println(platform.getOperatingSystem().getName())); // prints 'windows'
            );
        }
    }

1. It is an error to have a read only property for a type other than a `@Managed` interface
1. The nested model element has the same constraints as the parent

#### Test Coverage

- ~~Nested element is not `@Managed` causes error~~
- ~~Nested element violates constraints (error message indicates that it's being considered due to being nested, and indicates why enclosing class was being considered)~~

### ~~Plugin creates model element of custom, reference having, type without supplying an implementation~~

    @Managed
    interface Platform {
        String getDisplayName();
        void setDisplayName(String name);
        
        OperatingSystem getOperatingSystem();
        void setOperatingSystem(OperatingSystem operatingSystem); // setter for @Managed type indicates it's a reference
    }
    
    @Managed 
    interface OperatingSystem {
        String getName();
        void setName(String name);
    }
    
    class RulePlugin {
        @Model
        void createOs(OperatingSystem os) {
          os.setName("windows");
        }
        
        @Model
        void createPlatform(Platform platform, OperatingSystem os) {
          platform.setDisplayName("Microsoft Windows")
          platform.setOperatingSystem(os)
        }
        
        @Mutate
        void addPersonTask(CollectionBuilder<Task> tasks, Platform platform) {
            tasks.create("echo", t -> 
              t.doLast(t2 -> System.out.println(platform.getOperatingSystem().getName())); // prints 'windows'
            );
        }
    }

#### Test Coverage

- ~~Calling `setOperatingSystem()` with “non managed” impl of `OperatingSystem` is a runtime error (i.e. only managed objects can be used)~~


### ~~Plugin creates model element of custom, composite, type without supplying an implementation with a cyclical type reference~~

    The story makes the following possible:
    
    @Managed
    interface Parent {
        String getName();
        void setName(String name);
        
        Child getChild();
    }
    
    @Managed 
    interface Child {
        Parent getParent();
        void setParent(Parent parent);
    }
    
    class RulePlugin {
        @Model
        void createParent(Parent parent) {
            parent.setName("parent");
            parent.getChild().setParent(parent)
        }
        
        @Mutate
        void addEchoTask(CollectionBuilder<Task> tasks, Parent parent) {
            tasks.create("echo", t -> 
              t.doLast(t2 -> System.out.println(parent.getChild().getParent().getName())); // prints "parent"
            );
        }
    }

#### Test Coverage

- ~(something like snippet above)~
- ~should also support situations where more than two types are taking part in forming a cycle~

### ~~Plugin creates model element of custom type, containing properties of Java boxed primitive-ish types, without supplying an implementation~~

Adds support for:

1. `Boolean`
1. `Integer`
1. `Long`
1. `Double`
1. `BigInteger`
1. `BigDecimal`

Use of non primitive types is not allowed.
Attempt to declare a property of a primitive type should yield an error message indicating that a boxed type should be used instead.
 
1. boolean -> Boolean	
1. char -> Integer
1. float -> Double	
1. int -> Integer
1. long	-> Long
1. short -> Integer	
1. double -> Double

Use of other boxed types is not allowed.
Attempt to declare a property of a such a type should yield an error message indicating that an alternative type should be used (see mappings above).

Use of `byte` and `Byte` is unsupported. 

#### Test coverage

- ~~Can get/set properties of all supported types~~
- ~~Can narrow/widen values as per normal (e.g. set a `Long` property with a literal `int`)~~

#### Open questions

- Is this the right set of things to support? Should we just directly support all of Java's primitive types? 

### ~~Plugin creates model element of a collection of managed model elements~~

    @Managed
    interface Person {
      String getName(); void setName(String string)
    }
    
    package org.gradle.model.collection
    interface ManagedSet<T> extends Set<T> {
      void create(Action<? super T> action)
    }
    
    class Rules {
      @Model
      void people(ManagedSet<Person> people) {}
      
      @Mutate void addPeople(ManagedSet<Person> people) {
        people.create(p -> p.setName("p1"))
        people.create(p -> p.setName("p2"))
      }
    }
    
    model {
      people {
        create { it.name = "p3" }
      }
      
      tasks {
        create("printPeople") {
          it.doLast {
            assert $("people")*.name.sort() == ["p1", "p2", "p3"]
          }
        }
      }
    }
    
Notes:

- No lifecycle management at this stage (i.e. we don't prevent reading the collection when mutating and vice versa)
- All mutative methods of the `java.lang.Set` interface throw `UnsupportedOperationException`
    
#### Test coverage
    
- ~~Attempt to create collection of non managed type~~
- ~~Attempt to create collection of invalid managed type~~
    
### ~~Managed model interface extends other interfaces~~

    interface Named {
        String getName(); void setName(String name);         
    }
    
    @Managed
    interface NamedThing extends Named {
        String getValue(); void setValue(String value);
    }
    
#### Notes

- Super types do not need to be annotated with `@Managed` - but are subject to the same constraints as @Managed types
- Specialisation of a generic parent is not supported through this story (i.e. can't do `interface BookList extends List<Book>`)

#### Test Coverage

- ~~Can extend more than one interface~~
- ~~Error message produced when super type is not a “manageable” type indicates the original (sub) type (and the rule that caused it to be extracted)~~
- ~~Can get/set properties of super type(s)~~
- ~~Can depend on super type as input and subject~~
- ~~Two different types can extend same parent~~
- ~~Property conflicts between super types are detected (different types for the same name)~~ 

### ~~Managed model type has a property of collection of managed types~~

    @Managed
    interface Person {
      String getName(); void setName(String string)
    }
    
    @Managed
    interface Group {
      String getName(); void setName(String string)
      ManagedSet<Person> getMembers();
    }
    
    class Rules {
      @Model
      void group(Group group) {
        group.name = "Women in computing"
        group.members.create(p -> p.setName("Ada Lovelace"))
        group.members.create(p -> p.setName("Grace Hooper"))
      }
    }
    
    model {
      tasks {
        create("printGroup") {
          it.doLast {
            def members = $("group").members*.name.sort().join(", ")
            def name = $("group").name
            println "$name: $members"
          }
        }
      }
    }
    
#### Test Coverage

- ~~Something like the snippet above~~
- ~~Can set/get a reference to a collection of managed types~~
    
### ~~Managed model type has enum property~~

#### Notes

- Support for enums of any type
- Enum values are opaque to the model space (i.e. we do not treat enum values as structured objects, e.g. cannot depend on a property of an enum value)
- Enum values are not strictly immutable/threadsafe in Java but almost always are, as such we will consider them to be at this stage
- It doesn't have any impact at this stage, but only the enum value is strictly part of the model (all properties of an enum value are supplied by the runtime)

### Managed model element has unmanaged property

    interface MyModel {        
        @org.gradle.model.Unmanaged
        SomeWeirdThing getThing()        
        void setThing(SomeWeirdThing thing)
    }
    
Properties of an unmanaged type must be explicitly annotated with `@Unmanaged`.
The rationale for this is that the use of unmanaged properties will have a significant impact on tooling and functionality in general, as such it should be very clear to model consumers which properties are unmanaged.

Unmanaged properties must be accompanied by a setter.

#### Test Coverage

- ~~Can attach an an unmanaged property~~
- ~~Error when unmanaged property does not have annotation~~
- ~~Subtype may declare setter for unmanaged type~~
- Unmanaged property of managed type can be targeted for mutation
- Unmanaged property of managed type can be used as input
- Specific error when attempt is made to path into a managed type for mutation or as an input
    
### Managed model element has “generated” display name indicating identity in model space

    package org.example;
    
    @Managed
    interface Person {
      String getName(); void setName(String name);
    }
    
    @Managed
    interface Group {
      ManagedSet<Person> getPeople();
    }
    
    @RuleSource
    class Rules {
      @Model
      void g1(Group group) {
        group.getPeople().create(p -> p.setName("p1"));
        group.getPeople().create(p -> p.setName("p2"));
      }      
    }
    
    model {
      tasks {
        create("verify") {
          it.doLast { 
            assert $("group").people*.name.sort() == ["p1", "p2"]
          }
        }
      }
    }
    
#### Notes

- It is an error to define a setter for display name (may relax this in the future)
- Exact format of error message is unimportant, but it must include the “address” of the object in the model space
    
### Model rule accepts property of managed object as input
      
    @Managed
    interface Person {
      String getName(); void setName(String string)
    }
    
    class Rules {
      @Model
      void p1(Person person) {
        person.setName("foo");
      }
      
      @Mutate void addPeople(CollectionBuilder<Task> tasks, String personName) {
        tasks.create("injectedByType", t -> t.doLast(() -> assert personName.equals("foo"))
      }
    }
    
    model {
      tasks {
        create("injectedByName") {
          it.doLast {
            assert $("p1.name") == "foo"
          }
        }
      }
    }
    
### Test Coverage

- Can inject leaf type property (e.g. String, Number)
- Can inject node type property (i.e. another managed type with properties)
- Can inject property of property of managed type (i.e. given type `A` has property of managed type `B`, can inject properties of `B`) 
- Can inject by “path”
- Can inject by “type”

### Model rule mutates property of managed object
      
    @Managed
    interface Person {
      String getName(); void setName(String string)
      Person getMother();
      Person getFather();
    }
    
    class Rules {
      @Model
      void p1(Person person) {
        person.setName("foo");
      }
      
      @Mutate void setFather(@Path("p1.father") Person father) {
        father.setName("father")
      }
    }
    
    model {
      p1.mother { name = "mother" }
      tasks {
        create("test") {
          it.doLast {
            def p1 = $("p1")
            assert p1.mother.name == "mother"
            assert p1.father.name == "father"
          }
        }
      }
    }
        
#### Test Coverage

(above)

## Future candidate stories (unordered)

### Model designer augments generated display name to contain extra information

Possible driver for this is the component report.

### Plugin creates model element of custom type, containing a collecting of boxed primitive types, without supplying an implementation

### User assigns reference type property using indirect identifier

i.e. Something like the current scenario with `Platform.operatingSystem`. There is a set of objects of the referenced type, and they can be assigned as references some convenient way (e.g. by parsing a string)

### User is prevented from mutating managed model object when being used as an input

i.e. Something like the current scenario with `Platform.operatingSystem`. There is a set of objects of the referenced type, and they can be assigned as references some convenient way (e.g. by parsing a string)


### Plugin creates item of managed type in collection property of managed type
 
### Plugin creates item of managed type in collection property of unmanaged type
 
> Need to find a use case for this to see if it's needed (i.e. do we mix managed/unmanaged) types
 
### Model rule creates property of managed object

   @Managed
    interface Person {
      String getName(); void setName(String string)
      Person getFather();
    }
    
    class Rules {
      @Model
      void p1(Person person) {
        person.setName("foo");
      }
      
      @Model("p1.father")
      void father(Person father) {
        father.setName("father")
      }
    }
    
    model {
      tasks {
        create("test") {
          it.doLast {
            def p1 = $("p1")
            assert p1.father.name == "father"
          }
        }
      }
    }

#### Notes

Haven't identified a real use case for this yet. 
In theory, it allows avoiding creating the nested property if it's not needed.
However, given our current capabilities there's no real useful case for this.

### User sees useful type name in stack trace for managed model type and while debugging

This is about ensuring that managed model instances don't make debugging more difficult by presenting type names that don't hint at the type.