### GammaBus
GammaBus is a high-performance, easy-to-use, completely asynchronous pub-sub eventbus. This was created for a computer vision project I am actively working on, which has yet to be published.

The eventbus relies on lambdas, and, when calling events on the main thread, runs at speeds more than two orders of magnitude faster than Google Guava's eventbus. When calling asynchronous events, the eventbus runs at speeds comparable to Guava on the main thread.