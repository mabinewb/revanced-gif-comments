package app.revanced.gifcomments.patches

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.string
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction

val gifCommentsPatch = bytecodePatch(
    name = "GIF comments (Tenor ID)",
    description = "Posts a Tenor attachment from [[gif:ID]] in new comments and replies. Experimental; server acceptance requires device testing.",
    use = false,
) {
    compatibleWith("com.google.android.youtube"("20.40.45"))
    extendWith("gif-comments-extension.dex")
    apply {
        for (endpoint in listOf("comment/create_comment", "comment/create_comment_reply")) {
            val requestClass = classDefs.single { cls ->
                cls.methods.any { method ->
                    method.name == "<init>" && method.implementation?.instructions?.any {
                        it.string == endpoint
                    } == true
                }
            }
            val mutable = classDefs.getOrReplaceMutable(requestClass)
            val method = mutable.methods.single {
                it.name == "a" && it.parameterTypes.isEmpty() && it.returnType.startsWith("L") &&
                    it.implementation != null
            }
            val returns = method.implementation!!.instructions.withIndex().filter {
                it.value.opcode == Opcode.RETURN_OBJECT
            }.toList()
            check(returns.size == 1) { "Unexpected request builder structure for $endpoint" }
            val index = returns.single().index
            val register = (returns.single().value as OneRegisterInstruction).registerA
            method.addInstructions(index, """
                invoke-static/range { v$register .. v$register }, Lapp/revanced/gifcomments/GifComments;->rewrite(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$register
                check-cast v$register, ${method.returnType}
            """.trimIndent())
        }
    }
}
