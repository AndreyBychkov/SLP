package org.jetbrains.slp.modeling

import org.jetbrains.slp.modeling.mix.MixModel
import org.jetbrains.slp.modeling.ngram.NGramModel
import org.jetbrains.slp.modeling.runners.ModelRunner
import java.io.File

/**
 * Interface for models, providing the third step after lexing and translating.
 * Implemented primarily through [AbstractModel] and [MixModel].
 * <br></br><br></br>
 * The interface allows a model to be notified when modeling a new file and furthermore specifies
 * two types of updating (learning and forgetting) and two types of modeling (entropy and prediction).
 * Each update and model operation comes in two flavors: batch and indexed:
 *
 *  *  The **indexed** mode is essential for a model to support, allowing for such tasks
 * as on-the-fly prediction, updating cache models after seeing each token, etc.
 *  *  The **batch** mode is added because it can yield quite substantial speed-up for some models and
 * should thus be invoked as often as possible (e.g. as in [ModelRunner]).
 * It is implemented with simple iteration over calls to indexed mode by default but overriding this is encouraged.
 *
 * See also [AbstractModel], which overrides [.model] and [.predict] to incorporate dynamic
 * updating to models.
 * come with a default implementation which simply invokes the indexed version for each index.
 *
 * @author Vincent Hellendoorn
 */
interface Model {

    /**
     * Notifies model of upcoming test file, allowing it to set up accordingly (e.g. for nested models)
     * <br></br>
     * *Note:* most models may simply do nothing, but is tentatively left `abstract` as a reminder.
     *
     * @param next File to model next
     */
    fun notify(next: File)

    var dynamic: Boolean

    fun pauseDynamic()
    fun unPauseDynamic()

    /**
     * Notify underlying model to learn provided input. May be ignored if no such ability exists.
     * Default implementation simply invokes [.learnToken] for each position in input.
     *
     * @see {@link .learnToken
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     */
     fun learn(input: List<Int>) {
        (0 until input.size).forEach { learnToken(input, it) }
    }

    /**
     * Like [.learn] but for the specific token at `index`.
     * Primarily used for dynamic updating. Similar to [.modelToken],
     * batch implementation ([.learn] should be invoked when possible and can provide speed-up.
     *
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     * @param index Index of token to assign probability/confidence score too.
     */
    fun learnToken(input: List<Int>, index: Int)

    /**
     * Notify underlying model to 'forget' the provided input, e.g. prior to self-testing.
     * May be ignored if no such ability exists.
     * Default implementation simply invokes [.forgetToken] for each position in input.
     *
     * <br></br><br></br>
     * Any invoking code should note the risk of the underlying model not implementing this!
     * For instance when self-testing, this may lead to testing on org.jetbrains.slp.train-data.
     * <br></br>
     * See [slp.core.modeling.dynamic.NestedModel] for a good example, which uses this functionality to org.jetbrains.slp.train on all-but the test file.
     * It currently uses only [NGramModel]s which are capable of un-learning input.
     *
     * @see {@link .forgetToken
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     */
    fun forget(input: List<Int>) {
        (0 until input.size).forEach { forgetToken(input, it) }
    }

    /**
     * Like [.forget] but for the specific token at `index`.
     * Primarily used for dynamic updating. Similar to [.modelToken],
     * batch implementation ([.forget] should be invoked when possible and can provide speed-up.
     *
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     * @param index Index of token to assign probability/confidence score too.
     */
    fun forgetToken(input: List<Int>, index: Int)

    /**
     * Part of new interface design. Currently obsolete, do not use.
     */
    fun getConfidence(input: List<Int>, index: Int): Double {
        return 0.0
    }

    /**
     * Model each token in input to a pair of probability/confidence (see [.modelToken].
     * <br></br>
     * The default implementation simply invokes [.modelToken] for each index;
     * can be overridden in favor of batch processing by underlying class if preferable
     * (but remember to implement dynamic updating or caches won't work).
     *
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     *
     * @return Probability/Confidence Pair for each token in input
     */
    fun model(input: List<Int>): List<Pair<Double, Double>> {
        return (0 until input.size)
            .map { modelToken(input, it) }
    }

    /**
     * Model a single token in `input` at index `index` to a pair of (probability, confidence)  ([0,1], [0,1])
     * The probability must be a valid probability, positive and summing to 1 given the context.
     * <br></br>
     * [AbstractModel] implements this with dynamic updating support.
     * <br></br>
     * Since some models implement faster "batch" processing, [.model]
     * should generally be called if possible.
     *
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     * @param index Index of token to assign probability/confidence score too.
     *
     * @return Probability/Confidence Pair for token at `index` in `input`
     */
    fun modelToken(input: List<Int>, index: Int): Pair<Double, Double>

    /**
     * Give top `N` predictions for each token in input with probability/confidence scores (see [.modelToken].
     * <br></br>
     * The default implementation simply invokes [.predictToken] for each index;
     * can be overridden in favor of batch processing by underlying class if preferable
     * (but remember to implement dynamic updating or caches won't work).
     *
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     * @return Probability/Confidence-weighted set of predictions for each token in input
     */
    fun predict(input: List<Int>): List<Map<Int, Pair<Double, Double>>> {
        return (0 until input.size)
            .map { predictToken(input, it) }
    }

    /**
     * Give top `N` predictions for position `index` in input,
     * with corresponding probability/confidence scores as in [.modelToken].
     * <br></br>
     * [AbstractModel] implements this with dynamic updating support.
     * <br></br>
     * The model should produce suggestions for the token that should appear
     * following the first `index` tokens in `input`, regardless of what token is presently there
     * if any (e.g. it could be an insertion task).
     * Some example interpretations for different values of `index`:
     *
     *  *  0: predict the first token, without context.
     *  *  `input.size()`: predict the next token after `input`.
     *  *  3: predict the 4th token in `input`, regardless of what token is currently at that index.
     *
     * <br></br>
     * Since some models implement faster "batch" processing, [.predict]
     * should generally be called if possible.
     *
     * @param input Lexed and translated input tokens (use `Vocabulary` to translate back if needed)
     * @param index Index of token to assign probability/confidence score too.
     *
     * @return Probability/Confidence Pair for token at `index` in `input`
     */
    fun predictToken(input: List<Int>, index: Int): Map<Int, Pair<Double, Double>>

    fun save(directory: File)

    fun load(directory: File): Model


}
