import { useEffect, useState } from "react";
import type { ReactNode } from "react";
import { animationTime } from "./format";

export function Appear({ children }: { children: ReactNode }) {
  const [shown, setShown] = useState(children);
  const [previousChildren, setPreviousChildren] = useState(children);
  const [openCount, setOpenCount] = useState(0);

  if (children !== previousChildren) {
    setPreviousChildren(children);
  }

  if (previousChildren === null && children !== null) {
    setOpenCount(openCount + 1);
  }

  if (children !== null && children !== shown) {
    setShown(children);
  }

  useEffect(() => {
    if (children !== null) {
      return;
    }

    const timer = setTimeout(() => setShown(null), animationTime());
    return () => clearTimeout(timer);
  }, [children]);

  if (shown === null) {
    return null;
  }

  let className = "";

  if (children === null) {
    className = "closing";
  }

  return (
    <div key={openCount} className={className} inert={children === null}>
      {shown}
    </div>
  );
}
