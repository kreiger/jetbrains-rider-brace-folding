package com.linuxgods.kreiger.jetbrains.rider.brace.folding;

import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.jetbrains.rider.languages.fileTypes.csharp.kotoparser.lexer.CSharpTokenType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static com.intellij.psi.TokenType.WHITE_SPACE;

public class CSharpBraceFoldingBuilder extends FoldingBuilderEx {
    private static final Logger log = Logger.getInstance(CSharpBraceFoldingBuilder.class);

    @Override
    public FoldingDescriptor @NotNull [] buildFoldRegions(@NotNull PsiElement psiElement, @NotNull Document document, boolean b) {
        List<FoldingDescriptor> descriptors = new ArrayList<>();
        psiElement.accept(new PsiRecursiveElementVisitor() {

            @Override
            public void visitElement(@NotNull PsiElement element) {
                checkAstNode(element.getNode());
                super.visitElement(element);
            }

            private void checkAstNode(ASTNode astNode) {
                if (astNode.getElementType() != CSharpTokenType.LBRACE) return;
                ASTNode treePrev = astNode.getTreePrev();
                if (treePrev == null) {
                    ASTNode treeParent = astNode.getTreeParent();
                    if (treeParent == null) return;
                    treePrev = treeParent.getTreePrev();
                }
                if (treePrev == null || treePrev.getElementType() != WHITE_SPACE) return;
                TextRange textRange = treePrev.getTextRange();
                ASTNode treePrevPrev = treePrev.getTreePrev();
                if (treePrevPrev != null && treePrevPrev.getElementType() == WHITE_SPACE) {
                    textRange = treePrevPrev.getTextRange().union(textRange);
                } else if (textRange.getLength() == 1 && treePrev.getText().charAt(0) != '\n') {
                    return;
                }
                descriptors.add(new FoldingDescriptor(astNode, textRange));
            }
        });

        return descriptors.isEmpty() ? FoldingDescriptor.EMPTY_ARRAY : descriptors.toArray(FoldingDescriptor[]::new);
    }

    @Override
    public @Nullable String getPlaceholderText(@NotNull ASTNode astNode) {
        return " ";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        return true;
    }
}
