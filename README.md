# Source Language Processing

Library that explores source code to model it and 
make predictions using [N-gram method](https://shorturl.at/uFPTW).

Original idea and library [lays here](https://github.com/SLP-team/SLP-Core)

## Getting Started

### Setting up the dependency
You can do it in several ways

#### Gradle

We will be using [JitPack](https://jitpack.io) service

1. Add JitPack to repositories
```groovy
repositories {
    maven { url "https://jitpack.io" }
}
```


2. Add library to compile dependencies
```groovy
dependencies {
    implementation "com.github.AndreyBychkov:SLP:x"
}
```
and replace `x` with latest version number.

#### Manually

Download `jar` and source code from [latest release](https://github.com/AndreyBychkov/SLP/releases/tag/1.4.7)
and use your favorite way to add it as a dependency.

### First Model

```kotlin
val file = File("path/to/file.ext")

val manager = ModelRunnerManager()
val modelRunner = manager.getModelRunner(file.extension)

modelRunner.train(file)

val suggestion = modelRunner.getSuggestion("your source code")
println(suggestion)
```

Here we train a model on specified file
and make a suggestion of next token for inputs like `for (` or `System.out.`

### Pivotal classes

#### ModelRunnerManager
[ModelRunnerManager](src/main/kotlin/org/jetbrains/slp/modeling/ModelRunnerManager.kt) is a class that 
1. provides you Models for specified file's extension or [Language](src/main/kotlin/org/jetbrains/slp/Language.kt).
2. Contains and manages all your models.
3. Provides IO operations for save & load itself and thus containing models

Example
```kotlin
val storeDirectory = File("path/to/dir")

ModelRunnerManager().apply { 
    load(storeDirectory)
    getModelRunner(Language.JAVA).train("int i = 0;")
    save(storeDirectory)
}
```

#### ModelRunner
[ModelRunner](src/main/kotlin/org/jetbrains/slp/modeling/runners/ModelRunner.kt) and
[LocalGlobalModelRunner](src/main/kotlin/org/jetbrains/slp/modeling/runners/LocalGlobalModelRunner.kt)
are classes that wraps N-gram Models and 

1. provides `train` and `forget` operations for texts and files
2. provides flexible suggesting API for predicting next tokens

ModelRunner's aim is to build a pipeline with form
```
Input -> Lexer -> Vocabulary -> Model -> Vocabulary -> Reverse Lexer -> Output
```
so it requires 3 components:
1. LexerRunner
2. Vocabulary
3. Model

Providing custom components can help you customize ModelRunner for your own needs.

###### LocalGlobalRunner
LocalGlobalModelRunner is extension of ModelRunner which handles 2 different models: **Local** and **Global**.

We propose to use **Local** model in quickly changing contexts, like within a file.

On the contrary, we propose using  **Global** in large static contexts like modules or projects.

Together they generate more balanced suggestion than they do individually.


#### LexerRunner

[LexerRunner](src/main/kotlin/org/jetbrains/slp/lexing/LexerRunner.kt) is the class that manages Lexer
and implements lexing pipeline. 

Example
```kotlin
val lexerRunner = LexerRunnerFactory.getLexerRunner(Language.JAVA)
val code = "for (int i = 0; i != 10; ++i) {"

println(lexerRunner.lexLine(code).toList())
```
will generate list 
```
[<s>, for, (, int, i, =, 0, ;, i, !=, 10, ;, ++, i, ), {, </s>]
```

You can use `LexerRunnerFactory` to get predefined `LexerRunner` for implemented languages.

#### Vocabulary
[Vocabulary](src/main/kotlin/org/jetbrains/slp/translating/Vocabulary.kt) 
is the class that translates tokens to numbers for model. 

You will never interact with it directly but if you wish to manually control it's content
you can save & load it with [VocabularyRunner](src/main/kotlin/org/jetbrains/slp/translating/VocabularyRunner.kt)
and pass already filled vocabulary to ModelRunner.

#### Model

[Model](src/main/kotlin/org/jetbrains/slp/modeling/Model.kt) 
is the interface every model must implement so they can be used by ModelRunner. 

All our abstract models like [NGramModel](src/main/kotlin/org/jetbrains/slp/modeling/ngram/NGramModel.kt)
have static method `standart` which returns the generally best model in it's category.

If you want to mix your, for instance, neural network model with N-gram based, 
your model should implement `Model` interface 
and can be mixed with by [`MixModel`](src/main/kotlin/org/jetbrains/slp/modeling/mix/MixModel.kt)


### Extending API

If your language is not provided by SLP and you want to increase it's performance or appearance
we propose you to to following steps:

1. Implement [`Lexer`](src/main/kotlin/org/jetbrains/slp/lexing/Lexer.kt) 
to have control over tokens extraction.
2. Implement [`CodeFilter`](src/main/kotlin/org/jetbrains/slp/filters/CodeFilter.kt) 
to have control over output text appearance. This class translates tokens into text. 
Also, feel free to use some predefined filters from [`Filters`](src/main/kotlin/org/jetbrains/slp/filters/Filters.kt)
3. Add your language to [`Language`](src/main/kotlin/org/jetbrains/slp/Language.kt)
 and [`LexerRunnerFactory`](src/main/kotlin/org/jetbrains/slp/lexing/LexerRunnerFactory.kt) with it's file extensions.
4. Make a pull request.

Currently I working on removing need in pull request so can extend SLP directly on your project.




