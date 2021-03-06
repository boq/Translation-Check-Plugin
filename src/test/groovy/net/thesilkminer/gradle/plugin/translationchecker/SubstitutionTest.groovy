package net.thesilkminer.gradle.plugin.translationchecker

import net.thesilkminer.gradle.plugin.translationchecker.translation.TranslationFileTemplate
import net.thesilkminer.gradle.plugin.translationchecker.validation.ValidationMessage
import net.thesilkminer.gradle.plugin.translationchecker.validation.ValidationMessageAppender
import net.thesilkminer.gradle.plugin.translationchecker.validation.Validator

import org.junit.Test

class SubstitutionTest {

    static class Params{
        String template
        String translation
        String expected
        List<Validator> validators = []
        List<ValidationMessage> messages = []
    }

    static def testSubstitution(Closure setup) {
        def params = new Params()
        //setup.resolveStrategy = Closure.DELEGATE_FIRST
        setup.delegate = params
        setup()

        TranslationFileTemplate tft = new TranslationFileTemplate()
        tft.validators.addAll(params.validators)
        tft.parseTemplate(new StringReader(params.template))

        def outputBuffer = new StringWriter()
        tft.processTranslation(new StringReader(params.translation), new BufferedWriter(outputBuffer))

        def output = outputBuffer.toString().replace(System.getProperty("line.separator"), "\n")
        def expected = params.expected

        assert expected == null || output == expected
        for (message in params.messages)
            assert message in tft.validationMessages
    }

    @Test
    void testSingleSubstitution() {
        testSubstitution {
            template = "k=v"
            translation = "k=t"
            expected = "k=t\n"
        }
    }

    @Test
    void testUnicodeValue() {
        testSubstitution {
            template = "k=v"
            translation = "k=\u007Av"
            expected = "k=\u007Av\n"
        }
    }

    @Test
    void testMultipleSubstitutions() {
        testSubstitution {
            template = "k1=v1\nk2=v2"
            translation = "k2=t2\nk1=t1"
            expected = "k1=t1\nk2=t2\n"
        }
    }

    @Test
    void testMissingTranslation() {
         testSubstitution {
            template = "k1=v1\nk2=v2"
            translation = "k2=t2"
            expected = "#k1=v1 ## NEEDS TRANSLATION ##\nk2=t2\n"
        }
    }

    @Test
    void testCommentsAndWhitespaces() {
        testSubstitution {
            template = "#test\n\nk1=v1\n \n#@ \nk2=v2\n"
            translation = "#test\nk2=t2\nk1=t1"
            expected = "#test\n\nk1=t1\n \n#@ \nk2=t2\n"
        }
    }

    @Test
    void testAliases() {
         testSubstitution {
            template = "#@alias a\nk=v"
            translation = "a=t"
            expected = "k=t\n"
        }
    }

    @Test
    void testValidatorBasic() {
        testSubstitution {
            template = "#@alias a\nk=v"
            translation = "k=t"
            expected = "k=t\n"

            validators = [
                new Validator() {
                     def validateTemplate(Set<String> keys, String value, ValidationMessageAppender addMessage) {
                        addMessage(0, "template: " + keys.sort() + " = " + value)
                     }

                     def validateTranslation(String key, String value, ValidationMessageAppender addMessage) {
                        addMessage(0, "translation: " + key + " = " + value)
                     }
                }
            ]

            messages = [
                new ValidationMessage(source : "<stream>",
                                      key : "k",
                                      column : 0,
                                      message : "template: [a, k] = v"
                                      ),
                new ValidationMessage(source : "<stream>",
                                      key : "k",
                                      column : 0,
                                      message : "translation: k = t"
                                     ),
            ]
        }
    }

    @Test
    void testValidatorAliasesAndUnmatched() {
        testSubstitution {
            template = "#@alias a\nk1=v1\nk2=v2"
            translation = "a=t1\nx=t2"

            validators = [
                new Validator() {
                     def validateTemplate(Set<String> keys, String value, ValidationMessageAppender addMessage) {
                        addMessage(0, "template: " + keys.sort() + " = " + value)
                     }

                     def validateTranslation(String key, String value, ValidationMessageAppender addMessage) {
                        addMessage(0, "translation: " + key + " = " + value)
                     }
                }
            ]

            messages = [
                new ValidationMessage(source : "<stream>",
                                      key : "k1",
                                      column : 0,
                                      message : "template: [a, k1] = v1"
                                      ),
                new ValidationMessage(source : "<stream>",
                      key : "k2",
                      column : 0,
                      message : "template: [k2] = v2"
                      ),
                new ValidationMessage(source : "<stream>",
                                      key : "a",
                                      column : 0,
                                      message : "translation: a = t1"
                                     ),
                new ValidationMessage(source : "<stream>",
                                      key : "x",
                                      column : 0,
                                      message : "translation: x = t2"
                                     ),
            ]
        }
    }

    @Test
    void testEmptyEntries() {
        testSubstitution {
            template = "k=v"
            translation = "k"

            validators = [
                new Validator() {
                     def validateTemplate(Set<String> keys, String value, ValidationMessageAppender addMessage) {}

                     def validateTranslation(String key, String value, ValidationMessageAppender addMessage) {
                        addMessage(0, "translation: " + key + " = '" + value +"'")
                     }
                }
            ]

            messages = [
                new ValidationMessage(source : "<stream>",
                                      key : "k",
                                      column : 0,
                                      message : "translation: k = ''"
                                     ),
            ]
        }
    }
}