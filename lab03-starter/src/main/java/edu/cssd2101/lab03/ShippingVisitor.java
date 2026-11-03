package edu.cssd2101.lab03;

/** Stateless shipping operation: books cost 250 cents, magazines 200, downloads zero. */
public final class ShippingVisitor implements ComponentVisitor<Long> {
    /** Creates a visitor with no mutable running total. */
    public ShippingVisitor() {}

    @Override
    public Long visit(Leaf leaf) {
        return switch (leaf.material().kind()) {
            case BOOK -> 250L;
            case MAGAZINE -> 200L;
            case EBOOK, AUDIO, VIDEO -> 0L;
        };
    }

    @Override
    public Long visit(Bundle bundle) {
        long totalCents = 0L;
        for (Component child : bundle.children()) {
            long childShippingCents = child.accept(this);
            totalCents = Math.addExact(totalCents, childShippingCents);
        }
        return totalCents;
    }

    @Override
    public Long visit(AddOn addOn) {
        return addOn.wrapped().accept(this);
    }
}
