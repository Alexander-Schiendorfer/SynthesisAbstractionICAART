package de.uniaugsburg.isse.abstraction.merging;

import java.util.SortedSet;
import java.util.TreeSet;

import de.uniaugsburg.isse.abstraction.types.Interval;

/**
 * Prototypical solution for MergeLists hole detection algorithm
 * 
 * @author Alexander Schiendorfer
 * 
 */
public class MergeLists {

	public static class List {
		private Interval<Double> interval;
		private List next;

		public List(Interval<Double> intInterval, List nextObject) {
			this.setInterval(intInterval);
			this.setNext(nextObject);
		}

		public Interval<Double> getInterval() {
			return interval;
		}

		public void setInterval(Interval<Double> interval) {
			this.interval = interval;
		}

		public List getNext() {
			return next;
		}

		public void setNext(List next) {
			this.next = next;
		}

		public static List mergeIn(List head, Interval<Double> n) {
			List s = null;
			List l = head;
			List pred = null;
			List newInterval = new List(n, null);

			while (l != null && l.getInterval().max < n.min) {
				pred = l;
				l = l.getNext();
			}

			if (l == null) { // append
				pred.setNext(newInterval);
				return head;
			} else if (l.getInterval().min > n.max) {
				if (pred != null) {
					newInterval.setNext(l);
					pred.setNext(newInterval);
					return head;
				} else {
					newInterval.setNext(l);
					return newInterval;
				}
			} else { // starting to expand s = l
				s = l;
				s.getInterval().min = Math.min(s.getInterval().min, n.min);

				while (l != null && n.max >= l.getInterval().min) {
					s.getInterval().max = Math.max(s.getInterval().max, l.getInterval().max);
					l = l.getNext();
				}
				s.getInterval().max = Math.max(s.getInterval().max, n.max);
				s.setNext(l);
				// actually needs to delete all visited intervals -> left as an exercise for gc
				return head;
			}
		}

		public static List deepCopy(List head) {
			List newHead = head;
			List l = head, pred = null;
			while (l != null) {
				List newNode = new List(l.getInterval(), null);
				if (pred == null)
					newHead = newNode;
				else
					pred.setNext(newNode);
				pred = newNode;
				l = l.getNext();
			}
			return newHead;
		}

		public int size() {
			if (getNext() == null)
				return 1;
			else
				return 1 + getNext().size();
		}
	}

	public static List zero() {
		return new List(new Interval<Double>(0.0, 0.0), null);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		/*
		List l = new List(new Interval(4, 6), null);
		List head = l;
		List succ = new List(new Interval(9, 12), null);
		l.setNext(succ);
		l = succ;
		succ = new List(new Interval(15,17), null);
		l.setNext(succ);
		*/
		List l = new List(new Interval<Double>(0.0, 0.0), null);
		List head = l;
		head = List.mergeIn(head, new Interval<Double>(4.0, 6.0));
		head = List.mergeIn(head, new Interval<Double>(9., 12.));
		head = List.mergeIn(head, new Interval<Double>(15., 17.));

		// list [4 6] [9 12] [15 17]
		printList(head);
		System.out.println("Inserting 10, 15");
		head = List.mergeIn(head, new Interval<Double>(18., 23.));
		printList(head);
		System.out.println("Inserting 2 3");
		head = List.mergeIn(head, new Interval<Double>(2., 3.));
		printList(head);

		System.out.println("Inserting 1 10");
		head = List.mergeIn(head, new Interval<Double>(1., 10.));
		printList(head);

		System.out.println("Inserting 4 14");
		head = List.mergeIn(head, new Interval<Double>(4., 14.));
		printList(head);

		System.out.println("Inserting 5 16");
		head = List.mergeIn(head, new Interval<Double>(5., 16.));
		printList(head);

		System.out.println("Inserting 15 25");
		head = List.mergeIn(head, new Interval<Double>(15., 25.));
		printList(head);

	}

	public static void printList(List head) {
		List l = head;
		while (l != null) {
			System.out.print("[" + l.getInterval().min + " " + l.getInterval().max + "] ");
			l = l.getNext();
		}
		System.out.println();

	}

	public static SortedSet<Interval<Double>> toJavaSet(List l) {
		TreeSet<Interval<Double>> al = new TreeSet<Interval<Double>>();
		while (l != null) {
			al.add(l.getInterval());
			l = l.next;
		}
		return al;
	}

}
