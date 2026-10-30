package com.aoo.bcg.wordcard;

public record MingTangContext(int redCards,int bigCards,int smallCards,int concealedTriplets,int liftedQuads) {
    public MingTangContext { if(redCards<0||bigCards<0||smallCards<0||concealedTriplets<0||liftedQuads<0)throw new IllegalArgumentException("negative combination count"); }
    public int totalCards(){return bigCards+smallCards;}
}
