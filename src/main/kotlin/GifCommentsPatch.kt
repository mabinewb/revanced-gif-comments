package app.revanced.gifcomments.patches

import app.revanced.patcher.extensions.addInstructions
import app.revanced.patcher.extensions.string
import app.revanced.patcher.extensions.methodReference
import app.revanced.patcher.patch.bytecodePatch
import com.android.tools.smali.dexlib2.Opcode
import com.android.tools.smali.dexlib2.iface.instruction.OneRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.WideLiteralInstruction
import com.android.tools.smali.dexlib2.iface.instruction.FiveRegisterInstruction
import com.android.tools.smali.dexlib2.iface.instruction.RegisterRangeInstruction
import com.android.tools.smali.dexlib2.AccessFlags

val gifCommentsPatch = bytecodePatch(
    name = "GIF comments",
    description = "Adds a GIF button and Tenor search picker to the native comment editor. Experimental; posting requires device testing.",
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
                invoke-static/range { v$register .. v$register }, Lapp/revanced/gifcomments/GifComments;->rejectReserved(Ljava/lang/Object;)Ljava/lang/Object;
                move-result-object v$register
                check-cast v$register, ${method.returnType}
            """.trimIndent())
        }
        // Guard comment edit/delete/etc. builders so a hidden attachment marker
        // cannot be submitted as literal text by an unsupported comment endpoint.
        for (cls in classDefs.toList()) {
            val endpoint = cls.methods.filter { it.name == "<init>" }.flatMap {
                it.implementation?.instructions?.mapNotNull { instruction -> instruction.string } ?: emptyList()
            }.firstOrNull { it.startsWith("comment/") } ?: continue
            if(endpoint == "comment/create_comment" || endpoint == "comment/create_comment_reply") continue
            val mutable=classDefs.getOrReplaceMutable(cls)
            val method=mutable.methods.singleOrNull { it.name=="a" && it.parameterTypes.isEmpty() && it.returnType.startsWith("L") && it.implementation!=null } ?: continue
            val returns=method.implementation!!.instructions.withIndex().filter { it.value.opcode==Opcode.RETURN_OBJECT }.toList()
            for ((index,instruction) in returns.asReversed()) {
                val r=(instruction as OneRegisterInstruction).registerA
                method.addInstructions(index,"""
                    invoke-static/range { v$r .. v$r }, Lapp/revanced/gifcomments/GifComments;->rejectReserved(Ljava/lang/Object;)Ljava/lang/Object;
                    move-result-object v$r
                    check-cast v$r, ${method.returnType}
                """.trimIndent())
            }
        }
        var editorHooks=0
        for(cls in classDefs.toList()) {
            val candidates=cls.methods.filter { method -> method.implementation?.instructions?.any {
                (it as? WideLiteralInstruction)?.wideLiteral==0x7f0b040fL
            }==true }
            if(candidates.isEmpty())continue
            val mutable=classDefs.getOrReplaceMutable(cls)
            for(candidate in candidates) {
                val method=mutable.methods.single { it.name==candidate.name && it.parameterTypes==candidate.parameterTypes && it.returnType==candidate.returnType }
                val instructions=method.implementation!!.instructions.toList()
                val insertions=mutableListOf<Pair<Int,Int>>()
                for((index,instruction) in instructions.withIndex()) {
                    if((instruction as? WideLiteralInstruction)?.wideLiteral!=0x7f0b040fL)continue
                    val lookup=(index+1..minOf(index+6,instructions.size-2)).firstOrNull {
                        instructions[it].methodReference?.name=="findViewById"
                    } ?: continue
                    if(instructions[lookup+1].opcode!=Opcode.MOVE_RESULT_OBJECT)continue
                    insertions+=Pair(lookup+2,(instructions[lookup+1] as OneRegisterInstruction).registerA)
                }
                for((index,r) in insertions.distinct().sortedByDescending { it.first }) {
                    method.addInstructions(index,"invoke-static/range { v$r .. v$r }, Lapp/revanced/gifcomments/GifPicker;->install(Landroid/view/View;)V")
                    editorHooks++
                }
            }
        }
        check(editorHooks>0){"Native comment editor was not found"}
        println("GIF picker: installed $editorHooks native editor hooks")
        // Elements/full-screen composers may never look up the legacy comment ID.
        // Observe EditText construction, then qualify the attached editor at runtime.
        val editTypes=mutableSetOf("Landroid/widget/EditText;")
        var expanded=true
        while(expanded) {
            expanded=false
            for(cls in classDefs.toList()) if(cls.superclass in editTypes && editTypes.add(cls.type)) expanded=true
        }
        var constructedEditors=0
        for(cls in classDefs.toList()) {
            if(cls.type.startsWith("Lapp/revanced/gifcomments/"))continue
            val candidates=cls.methods.filter { method -> method.implementation?.instructions?.any {
                val ref=it.methodReference
                ref?.name=="<init>" && ref.definingClass in editTypes && ref.definingClass!=cls.type
            }==true }
            if(candidates.isEmpty())continue
            val mutable=classDefs.getOrReplaceMutable(cls)
            for(candidate in candidates) {
                val method=mutable.methods.single { it.name==candidate.name && it.parameterTypes==candidate.parameterTypes && it.returnType==candidate.returnType }
                val inserts=method.implementation!!.instructions.withIndex().mapNotNull { (index,instruction) ->
                    val ref=instruction.methodReference
                    if(ref?.name!="<init>" || ref.definingClass !in editTypes || ref.definingClass==cls.type) null
                    // For super() in an EditText subclass, defer until its own constructor returns.
                    else if(method.name=="<init>" && cls.type in editTypes) null
                    else {
                        val r=when(instruction) {
                            is FiveRegisterInstruction -> instruction.registerC
                            is RegisterRangeInstruction -> instruction.startRegister
                            else -> error("Unexpected constructor instruction")
                        }
                        Pair(index+1,r)
                    }
                }.toList()
                for((index,r) in inserts.sortedByDescending { it.first }) {
                    method.addInstructions(index,"invoke-static/range { v$r .. v$r }, Lapp/revanced/gifcomments/GifPicker;->observe(Landroid/view/View;)V")
                    constructedEditors++
                }
            }
            if(cls.type in editTypes) for(method in mutable.methods.filter { it.name=="<init>" && it.implementation!=null }) {
                val returns=method.implementation!!.instructions.withIndex().filter { it.value.opcode==Opcode.RETURN_VOID }.map { it.index }.toList()
                for(index in returns.asReversed()) {
                    method.addInstructions(index,"invoke-static/range { p0 .. p0 }, Lapp/revanced/gifcomments/GifPicker;->observe(Landroid/view/View;)V")
                    constructedEditors++
                }
            }
        }
        check(constructedEditors>0){"No EditText construction hooks found"}
        println("GIF picker: installed $constructedEditors construction hooks for full-screen editors")
        // Framework layout inflation can create a plain EditText without an app constructor call.
        // Cover activity decor roots and separate Dialog windows as well.
        val activity=classDefs.single { it.type=="Lcom/google/android/apps/youtube/app/watchwhile/MainActivity;" }
        val mainActivity=classDefs.getOrReplaceMutable(activity)
        val onCreate=mainActivity.methods.single { it.name=="onCreate" && it.parameterTypes==listOf("Landroid/os/Bundle;") }
        val activityReturns=onCreate.implementation!!.instructions.withIndex().filter { it.value.opcode==Opcode.RETURN_VOID }.map { it.index }.toList()
        for(index in activityReturns.asReversed())onCreate.addInstructions(index,"invoke-static/range { p0 .. p0 }, Lapp/revanced/gifcomments/GifPicker;->watchActivity(Landroid/app/Activity;)V")
        val dialogTypes=mutableSetOf("Landroid/app/Dialog;")
        expanded=true
        while(expanded) {
            expanded=false
            for(cls in classDefs.toList())if(cls.superclass in dialogTypes && dialogTypes.add(cls.type))expanded=true
        }
        var dialogHooks=0
        for(cls in classDefs.toList()) {
            if(cls.type.startsWith("Lapp/revanced/gifcomments/"))continue
            val candidates=cls.methods.filter { method -> method.implementation?.instructions?.any {
                val ref=it.methodReference
                ref?.name=="show" && ref.parameterTypes.isEmpty() && ref.definingClass in dialogTypes
            }==true }
            if(candidates.isEmpty())continue
            val mutable=classDefs.getOrReplaceMutable(cls)
            for(candidate in candidates) {
                val method=mutable.methods.single { it.name==candidate.name && it.parameterTypes==candidate.parameterTypes && it.returnType==candidate.returnType }
                val inserts=method.implementation!!.instructions.withIndex().mapNotNull { (index,instruction) ->
                    val ref=instruction.methodReference
                    if(ref?.name!="show" || ref.parameterTypes.isNotEmpty() || ref.definingClass !in dialogTypes)null
                    else Pair(index+1,when(instruction) {
                        is FiveRegisterInstruction -> instruction.registerC
                        is RegisterRangeInstruction -> instruction.startRegister
                        else -> error("Unexpected dialog show instruction")
                    })
                }.toList()
                for((index,r) in inserts.sortedByDescending { it.first }) {
                    method.addInstructions(index,"invoke-static/range { v$r .. v$r }, Lapp/revanced/gifcomments/GifPicker;->watchDialog(Landroid/app/Dialog;)V")
                    dialogHooks++
                }
            }
        }
        println("GIF picker: activity decor and $dialogHooks dialog window hooks")
        var transportHooks=0
        for(cls in classDefs.toList()) {
            val candidates=cls.methods.filter { method ->
                AccessFlags.STATIC.isSet(method.accessFlags) && method.parameterTypes.size>=3 &&
                    method.parameterTypes[1]=="Ljava/util/Map;" && method.parameterTypes[2]=="[B" &&
                    method.implementation?.instructions?.any {
                        it.methodReference?.definingClass=="Lorg/chromium/net/CronetEngine;" && it.methodReference?.name=="newUrlRequestBuilder"
                    }==true
            }
            if(candidates.isEmpty())continue
            val mutable=classDefs.getOrReplaceMutable(cls)
            for(candidate in candidates) {
                val method=mutable.methods.single { it.name==candidate.name && it.parameterTypes==candidate.parameterTypes && it.returnType==candidate.returnType }
                method.addInstructions(0,"""
                    invoke-static/range { p0 .. p2 }, Lapp/revanced/gifcomments/GifTransport;->rewrite(Ljava/lang/Object;Ljava/util/Map;[B)[B
                    move-result-object p2
                    invoke-static/range { p1 .. p2 }, Lapp/revanced/gifcomments/GifTransport;->headers(Ljava/util/Map;[B)Ljava/util/Map;
                    move-result-object p1
                """.trimIndent())
                println("GIF transport: ${method.definingClass}->${method.name}")
                transportHooks++
            }
        }
        check(transportHooks==1){"Expected exactly one Cronet request transport; found $transportHooks"}
    }
}
