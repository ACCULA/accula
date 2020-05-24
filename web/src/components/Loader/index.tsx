import React from 'react'
import ReactLoader from 'react-loader-spinner'

export const Loader = () => (
  <div className="text-center" style={{ margin: 30 }}>
    <ReactLoader
      type="TailSpin" //
      color="#2178A3"
      height={60}
      width={60}
    />
  </div>
)
