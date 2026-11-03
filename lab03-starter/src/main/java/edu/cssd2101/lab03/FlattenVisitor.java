package edu.cssd2101.lab03;

import java.util.ArrayList;
import java.util.List;

/** Stateless Visitor collecting every material occurrence in depth-first order. */
public final class FlattenVisitor implements ComponentVisitor<List<Material>> {
    /** Creates a reusable stateless visitor. */
    public FlattenVisitor() {}

    @Override
    public List<Material> visit(Leaf leaf) {
        return List.of(leaf.material());
    }

    @Override
    public List<Material> visit(Bundle bundle) {
        var materials = new ArrayList<Material>();
        for (Component child : bundle.children()) {
            materials.addAll(child.accept(this));
        }
        return List.copyOf(materials);
    }

    @Override
    public List<Material> visit(AddOn addOn) {
        return addOn.wrapped().accept(this);
    }
}
