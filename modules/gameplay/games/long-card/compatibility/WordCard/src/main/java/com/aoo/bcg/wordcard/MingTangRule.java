package com.aoo.bcg.wordcard;

import java.util.Optional;

public interface MingTangRule { int priority(); Optional<MingTang> evaluate(MingTangContext context); }
