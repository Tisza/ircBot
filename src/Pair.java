
public class Pair<K extends Comparable<K>, V extends Comparable<V>> implements 
	Comparable<Pair<K, V>> {

	public K k;
	public V v;
	
	public Pair(K key, V value) {
		this.k = key;
		this.v = value;
	}

	@Override
	public int compareTo(Pair<K, V> o) {
		if (o.k.compareTo(k) == 0) {
			return o.v.compareTo(v);
		}
		return o.k.compareTo(k);
	}
}
